package com.sorted.rest.services.ticket.controller;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.constants.Operation;
import com.sorted.rest.common.dbsupport.pagination.FilterCriteria;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest.SortDirection;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.params.service.ParamService;
import com.sorted.rest.services.ticket.beans.*;
import com.sorted.rest.services.ticket.clients.TicketClientService;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import com.sorted.rest.services.ticket.constants.TicketConstants.*;
import com.sorted.rest.services.ticket.entity.TicketCategoryEntity;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.services.TicketCategoryService;
import com.sorted.rest.services.ticket.services.TicketItemService;
import com.sorted.rest.services.ticket.services.TicketService;
import com.sorted.rest.services.ticket.utils.TicketActionUtils;
import com.sorted.rest.services.ticket.utils.TicketRequestUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@Api(tags = "Ticket Services")
public class TicketController implements BaseController {

	AppLogger _LOGGER = LoggingManager.getLogger(TicketController.class);

	@Autowired
	private TicketService ticketService;

	@Autowired
	private TicketItemService ticketItemService;

	@Autowired
	private TicketCategoryService ticketCategoryService;

	@Autowired
	private BaseMapper<?, ?> mapper;

	@Autowired
	private TicketClientService ticketClientService;

	@Autowired
	private ParamService paramService;

	@Autowired
	private TicketActionUtils ticketActionUtils;

	@Autowired
	private TicketRequestUtils ticketRequestUtils;

	@ApiOperation(value = "create tickets for ims", nickname = "createTicketsForIms")
	@PostMapping(path = "/tickets/ims")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(propagation = Propagation.REQUIRED)
	public void createTicketsForIms(@Valid @RequestBody ImsCreateTicketRequest createTicketBean) {
		_LOGGER.info(String.format("createTicketsForIms:: request %s", createTicketBean));
		if (EntityType.fromString(createTicketBean.getRequesterEntityType()) == null) {
			throw new ValidationException(
					ErrorBean.withError(Errors.INVALID_REQUEST, String.format("Invalid Entity Type : %s", createTicketBean.getRequesterEntityType()),
							"invalidEntityType"));
		}

		TicketEntity requestTicket = getMapper().mapSrcToDest(createTicketBean, TicketEntity.newInstance());
		if (requestTicket == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No data given to create ticket", null));
		}

		if (requestTicket.getRequesterEntityType().equals(EntityType.STORE.toString())) {
			requestTicket.setRequesterEntityCategory(getStoreCategoryForTicket(requestTicket.getRequesterEntityId(), requestTicket.getRequesterEntityType()));
		}

		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.findAllRecords();
		Map<Integer, TicketCategoryEntity> categoryMap = ticketCategoryEntities.stream()
				.collect(Collectors.toMap(TicketCategoryEntity::getId, Function.identity(), (o1, o2) -> o1, HashMap::new));
		validateAndSetTicketCategories(requestTicket, categoryMap);
		TicketEntity existingTicket = getParentAndValidateForDuplicateTickets(requestTicket);
		List<TicketItemEntity> requestTicketItems = requestTicket.getItems();
		if (existingTicket != null) {
			requestTicket = existingTicket;
		} else {
			requestTicket.setItems(null);
		}
		requestTicket.setHasNew(true);
		populateNewTicketItems(requestTicketItems, TicketPlatform.IMS);
		populateTicketDetailsAndInvokeCreateOrUpdateActions(requestTicket, requestTicketItems);
		ticketService.saveTicketWithItems(requestTicket, requestTicketItems);
	}

	// todo: API not in use, to be integrated in subsequent releases
	@ApiOperation(value = "create tickets for partner app", nickname = "createTicketsForPartnerApp")
	@PostMapping(path = "/tickets/partner-app")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(propagation = Propagation.REQUIRED)
	public void createTicketsForPartnerApp(@Valid @RequestBody PartnerAppCreateTicketRequest createTicketBean) {
		_LOGGER.info(String.format("createTicketsForPartnerApp:: request %s", createTicketBean));
		String requesterEntityId = SessionUtils.getStoreId();
		if (requesterEntityId == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Store id not given", null));
		}

		TicketEntity requestTicket = getMapper().mapSrcToDest(createTicketBean, TicketEntity.newInstance());
		if (requestTicket == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No data given to create ticket", null));
		}

		String requesterEntityType = EntityType.STORE.toString();
		requestTicket.setRequesterEntityId(requesterEntityId);
		requestTicket.setRequesterEntityType(requesterEntityType);
		requestTicket.setRequesterEntityCategory(getStoreCategoryForTicket(requesterEntityId, requesterEntityType));

		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.findAllRecords();
		Map<Integer, TicketCategoryEntity> categoryMap = ticketCategoryEntities.stream()
				.collect(Collectors.toMap(TicketCategoryEntity::getId, Function.identity(), (o1, o2) -> o1, HashMap::new));
		validateAndSetTicketCategories(requestTicket, categoryMap);
		TicketEntity existingTicket = getParentAndValidateForDuplicateTickets(requestTicket);
		List<TicketItemEntity> requestTicketItems = requestTicket.getItems();
		if (existingTicket != null) {
			requestTicket = existingTicket;
		} else {
			requestTicket.setItems(null);
		}
		requestTicket.setHasNew(true);
		populateNewTicketItems(requestTicketItems, TicketPlatform.PARTNER_APP);
		populateTicketDetailsAndInvokeCreateOrUpdateActions(requestTicket, requestTicketItems);
		ticketService.saveTicketWithItems(requestTicket, requestTicketItems);
	}

	// todo: API not in use, to be integrated in subsequent releases
	@ApiOperation(value = "create tickets for middle mile app", nickname = "createTicketsForMiddleMileApp")
	@PostMapping(path = "/tickets/middle-mile-app")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(propagation = Propagation.REQUIRED)
	public void createTicketsForMiddleMileApp(@Valid @RequestBody MiddleMileAppCreateTicketRequest createTicketBean) {
		_LOGGER.info(String.format("createTicketsForMiddleMileApp:: request %s", createTicketBean));
		if (EntityType.fromString(createTicketBean.getRequesterEntityType()) == null) {
			throw new ValidationException(
					ErrorBean.withError(Errors.INVALID_REQUEST, String.format("Invalid Entity Type : %s", createTicketBean.getRequesterEntityType()),
							"invalidEntityType"));
		}

		TicketEntity requestTicket = getMapper().mapSrcToDest(createTicketBean, TicketEntity.newInstance());
		if (requestTicket == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No data given to create ticket", null));
		}

		if (requestTicket.getRequesterEntityType().equals(EntityType.STORE.toString())) {
			requestTicket.setRequesterEntityCategory(getStoreCategoryForTicket(requestTicket.getRequesterEntityId(), requestTicket.getRequesterEntityType()));
		}

		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.findAllRecords();
		Map<Integer, TicketCategoryEntity> categoryMap = ticketCategoryEntities.stream()
				.collect(Collectors.toMap(TicketCategoryEntity::getId, Function.identity(), (o1, o2) -> o1, HashMap::new));
		validateAndSetTicketCategories(requestTicket, categoryMap);
		TicketEntity existingTicket = getParentAndValidateForDuplicateTickets(requestTicket);
		List<TicketItemEntity> requestTicketItems = requestTicket.getItems();
		if (existingTicket != null) {
			requestTicket = existingTicket;
		} else {
			requestTicket.setItems(null);
		}
		requestTicket.setHasNew(true);
		populateNewTicketItems(requestTicketItems, TicketPlatform.MIDDLE_MILE_APP);
		populateTicketDetailsAndInvokeCreateOrUpdateActions(requestTicket, requestTicketItems);
		ticketService.saveTicketWithItems(requestTicket, requestTicketItems);
	}

	private String getStoreCategoryForTicket(String storeId, String entityType) {
		List<String> storeCategoryForTicketParam = Arrays.stream(paramService.getParam("STORE_CATEGORY_FOR_TICKET", "Good|Good,Bad,Ugly").split("\\|"))
				.collect(Collectors.toList());
		return ticketClientService.getFilteredOrDefaultAudience(entityType, storeId,
				Arrays.stream(storeCategoryForTicketParam.get(1).split(",")).filter(s -> !StringUtils.isEmpty(s) && !StringUtils.isEmpty(s.trim()))
						.map(String::trim).distinct().collect(Collectors.toList()), storeCategoryForTicketParam.get(0));
	}

	private void validateAndSetTicketCategories(TicketEntity ticket, Map<Integer, TicketCategoryEntity> categoryMap) {
		if (!categoryMap.containsKey(ticket.getCategoryRootId())) {
			throw new ValidationException(
					ErrorBean.withError(Errors.NO_DATA_FOUND, String.format("Ticket category with id : %d not found, please retry", ticket.getCategoryRootId()),
							"categoryNotFound"));
		}
		ticket.setCategoryRoot(categoryMap.get(ticket.getCategoryRootId()));

		for (TicketItemEntity ticketItemEntity : ticket.getItems()) {
			if (!categoryMap.containsKey(ticketItemEntity.getCategoryLeafId())) {
				throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND,
						String.format("Ticket category with id : %d not found, please retry", ticketItemEntity.getCategoryLeafId()), "categoryNotFound"));
			}
			ticketItemEntity.setCategoryLeaf(categoryMap.get(ticketItemEntity.getCategoryLeafId()));
		}
	}

	private TicketEntity getParentAndValidateForDuplicateTickets(TicketEntity requestTicket) {
		TicketEntity dbEntity = null;
		if (requestTicket.getRequesterEntityType().equals(EntityType.STORE.toString())) {
			if (requestTicket.getCategoryRoot().getLabel().equals(TicketCategoryRoot.ORDER_ISSUE.toString())) {
				if (StringUtils.isEmpty(requestTicket.getReferenceId())) {
					throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Ticket reference id can not be empty", "referenceIdNotFound"));
				}
				dbEntity = ticketService.findByReferenceIdAndCategoryRootId(requestTicket.getReferenceId(), requestTicket.getCategoryRootId()).get(0);
				if (dbEntity != null) {
					Set<String> skuCodes = dbEntity.getItems().stream().filter(item -> !item.getStatus().equals(TicketStatus.CLOSED) && !item.getStatus()
									.equals(TicketStatus.CANCELLED) && item.getDetails().getOrderDetails() != null)
							.map(item -> item.getDetails().getOrderDetails().getSkuCode())
							.filter(skuCode -> !StringUtils.isEmpty(skuCode) && !StringUtils.isEmpty(skuCode.trim())).collect(Collectors.toSet());
					for (TicketItemEntity item : requestTicket.getItems()) {
						if (item.getDetails().getOrderDetails() != null && item.getDetails().getOrderDetails().getSkuCode() != null && skuCodes.contains(
								item.getDetails().getOrderDetails().getSkuCode())) {
							throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
									String.format("Ticket already exists for orderId : %s and skuCode : %s", requestTicket.getReferenceId(),
											item.getDetails().getOrderDetails().getSkuCode()), "duplicateTicket"));
						}
					}
				}
				//			todo: tickets for PAYMENT_ISSUE with referenceId not allowed in V1, add in subsequent releases
				//			} else if (requestTicket.getCategoryRoot().getLabel().equals(TicketCategoryRoot.PAYMENT_ISSUE.toString())) {
				//				if (requestTicket.getReferenceId() != null) {
				//					dbEntity = ticketService.findByReferenceIdAndCategoryRootId(requestTicket.getReferenceId(), requestTicket.getCategoryRootId()).get(0);
				//					if (dbEntity != null && !dbEntity.getItems().isEmpty()) {
				//						throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
				//								String.format("Ticket already exists for payment referenceId : %s", requestTicket.getReferenceId()), "duplicateTicket"));
				//					}
				//				}
			}
		}
		return dbEntity;
	}

	private void populateNewTicketItems(List<TicketItemEntity> requestTicketItems, TicketPlatform platform) {
		for (TicketItemEntity item : requestTicketItems) {
			item.setStatus(getTicketStatus(item.getCategoryLeaf().getIsTerminal(), item.getCategoryLeaf().getDescription()));
			item.setPlatform(platform.toString());
			item.setPriority(item.getCategoryLeaf().getPriority());
			item.setAssignedTeam(TicketResolutionTeam.CUSTOMERCARE.toString());
			item.setAssignedAt(new Date());
		}
	}

	private TicketStatus getTicketStatus(Integer isCategoryLeafTerminal, String categoryLeafDescription) {
		TicketStatus status = null;
		if (isCategoryLeafTerminal == -1) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
					String.format("Can not save ticket with category : %s. Please retry with other category", categoryLeafDescription), null));
		} else if (isCategoryLeafTerminal == 0) {
			status = TicketStatus.DRAFT;
		} else if (isCategoryLeafTerminal == 1) {
			status = TicketStatus.IN_PROGRESS;
		}
		return status;
	}

	private void populateTicketDetailsAndInvokeCreateOrUpdateActions(TicketEntity requestTicket, List<TicketItemEntity> requestTicketItems) {
		try {
			ticketRequestUtils.populateTicketRequestAsPerCategoryRoot(requestTicket, requestTicketItems);
			ticketActionUtils.populateTicketResolutionAsPerCategoryRoot(requestTicket, requestTicketItems);
			Boolean hasNew = requestTicket.getHasNew(), hasUpdatedDraft = requestTicket.getHasUpdatedDraft();
			if (requestTicket.getId() == null) {
				requestTicket.setLastAddedAt(new Date());
				requestTicket = ticketService.saveNewParentTicket(requestTicket);
				ticketActionUtils.addParentTicketHistory(requestTicket, hasNew, null, null, null, null);
			}

			if (hasNew) {
				for (TicketItemEntity item : requestTicketItems) {
					item.setTicket(requestTicket);
				}
				requestTicket.setLastAddedAt(new Date());
				requestTicketItems = ticketItemService.saveAll(requestTicketItems);
			}

			for (TicketItemEntity item : requestTicketItems) {
				if (hasNew) {
					ticketActionUtils.invokeTicketCreateAction(item, requestTicket.getId());
				} else if (hasUpdatedDraft) {
					ticketActionUtils.invokeDraftTicketUpdateAction(item, requestTicket.getId());
				}

				if (item.getStatus().equals(TicketStatus.IN_PROGRESS)) {
					ticketActionUtils.invokeTicketRaiseAction(item, requestTicket.getId());
				}
			}
			ticketRequestUtils.clearTicketRequest();
		} catch (Exception e) {
			ticketRequestUtils.clearTicketRequest();
			_LOGGER.error("Something went wrong while creating ticket ", e);
			throw e;
		}
	}

	private void setTicketActionsAndCategory(TicketItemBean itemBean, Integer categoryRootId, List<TicketCategoryEntity> ticketCategoryEntities) {
		itemBean.setUpdateActions(itemBean.getCategoryLeaf().getOnUpdateActions());
		itemBean.setCategory(ticketCategoryService.getRootToLeafPathUsingCategoryList(ticketCategoryEntities, categoryRootId, itemBean.getCategoryLeafId()));
	}

	@ApiOperation(value = "update ticket from draft for ims", nickname = "updateTicketFromDraftForIms")
	@PutMapping(path = "/tickets/ims/draft")
	@ResponseStatus(HttpStatus.OK)
	@Transactional(propagation = Propagation.REQUIRED)
	public TicketItemBean updateTicketFromDraftForIms(@Valid @RequestBody UpdateTicketFromDraftBean updateTicketBean) {
		_LOGGER.info(String.format("updateTicketFromDraftForIms:: request %s", updateTicketBean));
		TicketEntity ticket = ticketService.findById(updateTicketBean.getId());
		if (ticket == null) {
			throw new ValidationException(
					ErrorBean.withError(Errors.NO_DATA_FOUND, String.format("No data found for ticket with id : %s", updateTicketBean.getItemId()), null));
		}
		Optional<TicketItemEntity> itemOptional = ticket.getItems().stream().filter(i -> Objects.equals(i.getId(), updateTicketBean.getItemId())).findFirst();
		if (itemOptional.isEmpty()) {
			throw new ValidationException(
					ErrorBean.withError(Errors.NO_DATA_FOUND, String.format("No data found for ticket item with id : %s", updateTicketBean.getItemId()), null));
		}
		if (!itemOptional.get().getStatus().equals(TicketStatus.DRAFT)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
					String.format("Ticket item with id : %s already moved from DRAFT. Current status : %s", updateTicketBean.getId(),
							itemOptional.get().getStatus()), null));
		}
		TicketItemEntity item = itemOptional.get();
		if (updateTicketBean.getCategoryLeafId() != null && !updateTicketBean.getCategoryLeafId().equals(item.getCategoryLeafId())) {
			TicketCategoryEntity categoryLeaf = ticketCategoryService.findRecordById(updateTicketBean.getCategoryLeafId());
			if (categoryLeaf == null) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
						String.format("No data found for ticket category with id : %s", updateTicketBean.getCategoryLeafId()), null));
			}
			item.setCategoryLeaf(categoryLeaf);
			item.setPriority(item.getCategoryLeaf().getPriority());
			item.setStatus(getTicketStatus(item.getCategoryLeaf().getIsTerminal(), item.getCategoryLeaf().getDescription()));
		}
		if (updateTicketBean.getAttachments() != null && !updateTicketBean.getAttachments().isEmpty()) {
			item.setNewAttachments(updateTicketBean.getAttachments());
			item.setAttachments(Stream.concat(item.getAttachments().stream(), updateTicketBean.getAttachments().stream()).collect(Collectors.toList()));
		}
		if (updateTicketBean.getDescription() != null) {
			item.getDetails().setDescription(updateTicketBean.getDescription());
		}
		ticket.setHasUpdatedDraft(true);
		populateTicketDetailsAndInvokeCreateOrUpdateActions(ticket, Collections.singletonList(item));
		ticket = ticketService.saveTicketWithUpdatedItems(ticket);
		itemOptional = ticket.getItems().stream().filter(i -> Objects.equals(i.getId(), updateTicketBean.getItemId())).findFirst();
		if (itemOptional.isPresent()) {
			TicketItemBean itemBean = getMapper().mapSrcToDest(itemOptional.get(), TicketItemBean.newInstance());
			List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.findAllRecords();
			setTicketActionsAndCategory(itemBean, ticket.getCategoryRootId(), ticketCategoryEntities);
			return itemBean;
		} else {
			throw new ValidationException(
					ErrorBean.withError(Errors.NO_DATA_FOUND, String.format("No data found after updating ticket with id : %s", updateTicketBean.getItemId()),
							null));
		}
	}

	@ApiOperation(value = "update ticket for backoffice", nickname = "updateTicketForIms")
	@PutMapping(path = "/tickets/ims")
	@ResponseStatus(HttpStatus.OK)
	@Transactional(propagation = Propagation.REQUIRED)
	public TicketItemBean updateTicketForIms(@Valid @RequestBody UpdateTicketBean updateTicketBean) {
		_LOGGER.info(String.format("updateTicketForIms:: request %s", updateTicketBean));
		TicketEntity ticket = ticketService.findById(updateTicketBean.getId());
		if (ticket == null) {
			throw new ValidationException(
					ErrorBean.withError(Errors.INVALID_REQUEST, String.format("No data found for ticket with id : %s", updateTicketBean.getId()), null));
		}
		Optional<TicketItemEntity> itemOptional = ticket.getItems().stream().filter(i -> Objects.equals(i.getId(), updateTicketBean.getItemId())).findFirst();
		if (itemOptional.isEmpty()) {
			throw new ValidationException(
					ErrorBean.withError(Errors.NO_DATA_FOUND, String.format("No data found for ticket item with id : %s", updateTicketBean.getItemId()), null));
		}
		if (!itemOptional.get().getStatus().equals(TicketStatus.IN_PROGRESS) && !(itemOptional.get().getStatus()
				.equals(TicketStatus.DRAFT) && updateTicketBean.getAction().equals(TicketUpdateActions.CANCEL_WITH_REMARKS.toString()))) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
					String.format("Ticket item with id : %s already moved from IN_PROGRESS. Current status : %s", updateTicketBean.getItemId(),
							itemOptional.get().getStatus()), null));
		}
		TicketItemEntity item = itemOptional.get();

		if (updateTicketBean.getAttachments() != null && !updateTicketBean.getAttachments().isEmpty()) {
			item.setNewAttachments(updateTicketBean.getAttachments());
			item.setAttachments(Stream.concat(item.getAttachments().stream(), updateTicketBean.getAttachments().stream()).collect(Collectors.toList()));
		}
		if (StringUtils.isEmpty(updateTicketBean.getRemarks())) {
			updateTicketBean.setRemarks(TicketConstants.UPDATED_TICKET_DEFAULT_REMARKS);
		}

		populateTicketDetailsAndInvokeUpdateActions(ticket, item, updateTicketBean);
		ticket = ticketService.saveTicketWithUpdatedItems(ticket);
		itemOptional = ticket.getItems().stream().filter(i -> Objects.equals(i.getId(), updateTicketBean.getItemId())).findFirst();
		if (itemOptional.isPresent()) {
			TicketItemBean itemBean = getMapper().mapSrcToDest(itemOptional.get(), TicketItemBean.newInstance());
			List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.findAllRecords();
			setTicketActionsAndCategory(itemBean, ticket.getCategoryRootId(), ticketCategoryEntities);
			return itemBean;
		} else {
			throw new ValidationException(
					ErrorBean.withError(Errors.NO_DATA_FOUND, String.format("No data found after updating ticket with id : %s", updateTicketBean.getItemId()),
							null));
		}
	}

	private void populateTicketDetailsAndInvokeUpdateActions(TicketEntity requestTicket, TicketItemEntity requestItem, UpdateTicketBean updateTicketBean) {
		try {
			ticketRequestUtils.populateTicketRequestAsPerCategoryRoot(requestTicket, Collections.singletonList(requestItem));
			ticketActionUtils.invokeTicketUpdateAction(requestItem, requestTicket.getId(), updateTicketBean);
			ticketRequestUtils.clearTicketRequest();
		} catch (Exception e) {
			ticketRequestUtils.clearTicketRequest();
			throw e;
		}
	}

	@ApiOperation(value = "fetch paginated lists of tickets for ims", nickname = "fetchTicketsIms")
	@GetMapping(path = "/tickets/ims")
	public PageAndSortResult<TicketListViewBean> fetchTicketsIms(@RequestParam(defaultValue = "1") Integer pageNo, @RequestParam(defaultValue = "25") Integer pageSize,
			@RequestParam(required = false) String sortBy, @RequestParam(required = false) PageAndSortRequest.SortDirection sortDirection,
			HttpServletRequest request, @RequestParam Boolean orderRelated, @RequestParam(required = false) String lastAddedOn,
			@RequestParam(required = false) Boolean hasDraft, @RequestParam(required = false) Boolean hasPending,
			@RequestParam(required = false) Boolean hasClosed) throws ParseException {
		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.findAllRecords();

		Map<String, SortDirection> sort;
		if (sortBy != null) {
			sort = buildSortMap(sortBy, sortDirection);
		} else {
			sort = new LinkedHashMap<>();
			sort.put("lastAddedAt", PageAndSortRequest.SortDirection.DESC);
		}
		final Map<String, Object> filters = getSearchParams(request, TicketEntity.class);
		updateFilters(filters, orderRelated, lastAddedOn, hasDraft, hasPending, hasClosed, ticketCategoryEntities);

		PageAndSortResult<TicketEntity> tickets = ticketService.getAllTicketsPaginated(pageSize, pageNo, filters, sort);
		PageAndSortResult<TicketListViewBean> response = prepareResponsePageData(tickets, TicketListViewBean.class);
		if (orderRelated) {
			Set<String> displayOrderIds = response.getData().stream().filter(ticket -> ticket.getMetadata().getOrderDetails() != null && !StringUtils.isEmpty(
							ticket.getMetadata().getOrderDetails().getDisplayOrderId())).map(ticket -> ticket.getMetadata().getOrderDetails().getDisplayOrderId())
					.collect(Collectors.toSet());
			Map<String, FranchiseOrderListBean> ordersDisplayIdMap = ticketClientService.getFranchiseOrderByDisplayIds(displayOrderIds).stream()
					.collect(Collectors.toMap(FranchiseOrderListBean::getDisplayOrderId, Function.identity()));
			for (TicketListViewBean ticketBean : response.getData()) {
				if (ticketBean.getMetadata().getOrderDetails() != null && ticketBean.getMetadata().getOrderDetails()
						.getDisplayOrderId() != null && ordersDisplayIdMap.containsKey(ticketBean.getMetadata().getOrderDetails().getDisplayOrderId())) {
					FranchiseOrderListBean orderListBean = ordersDisplayIdMap.get(ticketBean.getMetadata().getOrderDetails().getDisplayOrderId());
					ticketBean.getMetadata().getOrderDetails().setOrderStatus(orderListBean.getStatus());
				}
			}
		}
		return response;
	}

	private void updateFilters(Map<String, Object> filters, Boolean orderRelated, String lastAddedOn, Boolean hasDraft, Boolean hasPending, Boolean hasClosed,
			List<TicketCategoryEntity> ticketCategoryEntities) throws ParseException {

		Map<String, TicketCategoryEntity> categoryMap = ticketCategoryEntities.stream()
				.collect(Collectors.toMap(TicketCategoryEntity::getLabel, Function.identity(), (o1, o2) -> o1, HashMap::new));
		List<TicketCategoryEntity> categoryRootsIn = new ArrayList<>();
		if (orderRelated) {
			categoryRootsIn.add(categoryMap.get(TicketCategoryRoot.ORDER_ISSUE.toString()));
		} else {
			categoryRootsIn.add(categoryMap.get(TicketCategoryRoot.POS_ISSUE.toString()));
			categoryRootsIn.add(categoryMap.get(TicketCategoryRoot.PAYMENT_ISSUE.toString()));
			categoryRootsIn.add(categoryMap.get(TicketCategoryRoot.PRICING_ISSUE.toString()));
			categoryRootsIn.add(categoryMap.get(TicketCategoryRoot.APP_ISSUE.toString()));
		}
		List<Integer> categoryRootIds = new ArrayList<>();
		for (TicketCategoryEntity category : categoryRootsIn) {
			if (category != null && category.getId() != null)
				categoryRootIds.add(category.getId());
		}
		if (categoryRootIds.isEmpty()) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, "Any relevant issue ticket category not found", null));
		}
		filters.remove("orderRelated");
		filters.put("categoryRootId", categoryRootIds);

		if (lastAddedOn != null) {
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			formatter.setTimeZone(TimeZone.getDefault());
			Date fromDate = DateUtils.addMinutes(formatter.parse(lastAddedOn), -330);

			filters.put("fromDate", new FilterCriteria("lastAddedAt", fromDate, Operation.GTE));
			filters.put("toDate", new FilterCriteria("lastAddedAt", DateUtils.addDays(fromDate, 1), Operation.LTE));
			filters.remove("lastAddedOn");
		}

		if (hasDraft != null) {
			if (hasDraft) {
				filters.put("hasDraft", new FilterCriteria("draftCount", 0, Operation.GT));
			} else {
				filters.put("hasDraft", new FilterCriteria("draftCount", 0, Operation.EQUALS));
			}
		}

		if (hasPending != null) {
			if (hasPending) {
				filters.put("hasPending", new FilterCriteria("pendingCount", 0, Operation.GT));
			} else {
				filters.put("hasPending", new FilterCriteria("pendingCount", 0, Operation.EQUALS));
			}
		}

		if (hasClosed != null) {
			if (hasClosed) {
				filters.put("hasClosed", new FilterCriteria("closedCount", 0, Operation.GT));
			} else {
				filters.put("hasClosed", new FilterCriteria("closedCount", 0, Operation.EQUALS));
			}
		}
	}

	private void filterTicketOnShowDraft(Boolean showDraft, List<TicketBean> ticketBeans) {
		List<TicketBean> removeList = new ArrayList<>();

		if (showDraft) {
			for (TicketBean ticketBean : ticketBeans) {
				List<TicketItemBean> filteredItems = ticketBean.getItems().stream().filter(item -> item.getStatus().equals(TicketStatus.DRAFT.toString()))
						.collect(Collectors.toList());
				if (filteredItems.isEmpty()) {
					removeList.add(ticketBean);
				} else {
					ticketBean.setItems(filteredItems);
				}
			}
		} else {
			for (TicketBean ticketBean : ticketBeans) {
				List<TicketItemBean> filteredItems = ticketBean.getItems().stream().filter(item -> !item.getStatus().equals(TicketStatus.DRAFT.toString()))
						.collect(Collectors.toList());
				if (filteredItems.isEmpty()) {
					removeList.add(ticketBean);
				} else {
					ticketBean.setItems(filteredItems);
				}
			}
		}
		ticketBeans.removeAll(removeList);
	}

	@ApiOperation(value = "create or update tickets for store return", nickname = "createOrUpdateTicketsForStoreReturn")
	@PostMapping(path = "/tickets/store-return")
	@ResponseStatus(HttpStatus.OK)
	@Transactional(propagation = Propagation.REQUIRED)
	public void createOrUpdateTicketsForStoreReturn(@RequestBody @Valid StoreReturnTicketRequest request) {
		_LOGGER.info(String.format("createOrUpdateTicketsForStoreReturn:: request %s", request));
		if (request.getStoreId() == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Store id not given", null));
		}
		if (request.getOrderId() == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Order id not given", null));
		}

		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.findAllRecords();
		Map<String, TicketCategoryEntity> categoryMap = ticketCategoryEntities.stream()
				.collect(Collectors.toMap(TicketCategoryEntity::getLabel, Function.identity(), (o1, o2) -> o1, HashMap::new));
		if (!categoryMap.containsKey(TicketCategoryRoot.ORDER_ISSUE.toString()) || !categoryMap.containsKey(
				TicketConstants.STORE_RETURN_TICKET_CATEGORY_LEAF_LABEL)) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, "Relevant order issue ticket category not found", null));
		}
		TicketCategoryEntity categoryRoot = categoryMap.get(TicketCategoryRoot.ORDER_ISSUE.toString());
		TicketCategoryEntity categoryLeaf = categoryMap.get(TicketConstants.STORE_RETURN_TICKET_CATEGORY_LEAF_LABEL);
		Map<String, TicketItemEntity> ticketItemSkuMap = new HashMap<>();

		TicketEntity ticket = ticketService.findByReferenceIdAndCategoryRootId(request.getOrderId(), categoryRoot.getId()).get(0);

		if (ticket == null) {
			ticket = createTicketForStoreReturn(request, categoryRoot);
		} else {
			ticketItemSkuMap = ticket.getItems().stream()
					.filter(item -> !item.getStatus().equals(TicketStatus.CLOSED) && !item.getStatus().equals(TicketStatus.CANCELLED) && item.getDetails()
							.getOrderDetails() != null && item.getDetails().getOrderDetails().getSkuCode() != null)
					.collect(Collectors.toMap(item -> item.getDetails().getOrderDetails().getSkuCode(), Function.identity(), (o1, o2) -> o1, HashMap::new));
		}

		List<TicketItemEntity> insertTicketItems = new ArrayList<>();
		List<TicketItemEntity> updateTicketItems = new ArrayList<>();

		for (StoreReturnItemData requestItem : request.getItems()) {
			if (ticketItemSkuMap.containsKey(requestItem.getSkuCode())) {
				updateTicketItems.add(ticketItemSkuMap.get(requestItem.getSkuCode()));
			} else {
				TicketItemEntity ticketItem = createTicketItemForStoreReturn(requestItem, categoryLeaf);
				insertTicketItems.add(ticketItem);
			}
		}

		ticket.setHasNew(!insertTicketItems.isEmpty());
		if (ticket.getHasNew() || ticket.getId() == null) {
			populateTicketDetailsAndInvokeCreateOrUpdateActions(ticket, insertTicketItems);
		}

		ticketItemSkuMap = Stream.concat(updateTicketItems.stream(), insertTicketItems.stream())
				.collect(Collectors.toMap(item -> item.getDetails().getOrderDetails().getSkuCode(), Function.identity(), (o1, o2) -> o1, HashMap::new));

		for (StoreReturnItemData requestItem : request.getItems()) {
			if (ticketItemSkuMap.containsKey(requestItem.getSkuCode())) {
				ticketActionUtils.invokeUpdateStoreReturnInfoAction(ticketItemSkuMap.get(requestItem.getSkuCode()), ticket.getId(), requestItem);
			}
		}

		ticket.addTicketItems(insertTicketItems);
		for (TicketItemEntity item : ticket.getItems()) {
			item.setTicket(ticket);
		}
		ticketService.saveTicketWithUpdatedItems(ticket);
	}

	private TicketEntity createTicketForStoreReturn(StoreReturnTicketRequest request, TicketCategoryEntity category) {
		TicketEntity ticket = TicketEntity.newInstance();
		ticket.setReferenceId(request.getOrderId());
		ticket.setRequesterEntityId(request.getStoreId());
		ticket.setRequesterEntityType(EntityType.STORE.toString());
		ticket.setRequesterEntityCategory(getStoreCategoryForTicket(request.getStoreId(), EntityType.STORE.toString()));
		ticket.setCategoryRoot(category);
		return ticket;
	}

	private TicketItemEntity createTicketItemForStoreReturn(StoreReturnItemData requestItem, TicketCategoryEntity category) {
		ResolutionDetailsBean ticketDetailsBean = ResolutionDetailsBean.newInstance();
		ticketDetailsBean.setDescription(TicketConstants.STORE_RETURN_TICKET_DESCRIPTION);
		OrderItemDetailsBean orderDetailsRequestBean = OrderItemDetailsBean.newInstance();
		orderDetailsRequestBean.setSkuCode(requestItem.getSkuCode());
		orderDetailsRequestBean.setIssueQty(requestItem.getQuantity());
		ticketDetailsBean.setOrderDetails(orderDetailsRequestBean);

		TicketItemEntity ticketItem = TicketItemEntity.newInstance();
		ticketItem.setDetails(ticketDetailsBean);
		ticketItem.setCategoryLeafId(category.getId());
		ticketItem.setCategoryLeaf(category);
		ticketItem.setStatus(getTicketStatus(ticketItem.getCategoryLeaf().getIsTerminal(), ticketItem.getCategoryLeaf().getDescription()));
		ticketItem.setPlatform(TicketPlatform.STORE_RETURN.toString());
		ticketItem.setPriority(ticketItem.getCategoryLeaf().getPriority());
		ticketItem.setAssignedTeam(TicketResolutionTeam.CUSTOMERCARE.toString());
		ticketItem.setAssignedAt(new Date());
		return ticketItem;
	}

	@ApiOperation(value = "upload files", nickname = "uploadFiles")
	@PostMapping(path = "/tickets/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<List<String>> uploadFiles(@RequestParam("files") MultipartFile[] files) {
		return ResponseEntity.ok(ticketService.uploadFiles(files));
	}

	@ApiOperation(value = "fetch ticket for ims by id", nickname = "fetchTicketByIdIms")
	@GetMapping(path = "/tickets/{id}/ims")
	public ResponseEntity<TicketBean> fetchTicketByIdIms(@PathVariable Long id, @RequestParam(defaultValue = "false") Boolean showOnlyDraft) {
		TicketEntity ticket = ticketService.findById(id);
		if (ticket == null) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, String.format("No data found for ticket with id : %s", id), null));
		}
		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.findAllRecords();
		TicketBean ticketBean = getMapper().mapSrcToDest(ticket, TicketBean.newInstance());
		if (showOnlyDraft) {
			filterTicketOnShowDraft(true, Collections.singletonList(ticketBean));
		} else {
			filterTicketOnShowDraft(false, Collections.singletonList(ticketBean));
		}
		if (ticketBean.getCategoryRoot().getLabel().equals(TicketCategoryRoot.ORDER_ISSUE.toString())) {
			Set<String> displayOrderIds = ticketBean.getMetadata().getOrderDetails() != null && !StringUtils.isEmpty(
					ticketBean.getMetadata().getOrderDetails().getDisplayOrderId()) ?
					Collections.singleton(ticketBean.getMetadata().getOrderDetails().getDisplayOrderId()) :
					new HashSet<>();
			Map<String, FranchiseOrderListBean> ordersDisplayIdMap = ticketClientService.getFranchiseOrderByDisplayIds(displayOrderIds).stream()
					.collect(Collectors.toMap(FranchiseOrderListBean::getDisplayOrderId, Function.identity()));
			if (ticketBean.getMetadata().getOrderDetails() != null && ticketBean.getMetadata().getOrderDetails()
					.getDisplayOrderId() != null && ordersDisplayIdMap.containsKey(ticketBean.getMetadata().getOrderDetails().getDisplayOrderId())) {
				FranchiseOrderListBean orderListBean = ordersDisplayIdMap.get(ticketBean.getMetadata().getOrderDetails().getDisplayOrderId());
				ticketBean.getMetadata().getOrderDetails().setOrderStatus(orderListBean.getStatus());
			}
		}
		for (TicketItemBean itemBean : ticketBean.getItems()) {
			setTicketActionsAndCategory(itemBean, ticketBean.getCategoryRootId(), ticketCategoryEntities);
		}
		return ResponseEntity.ok(ticketBean);
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}
}
