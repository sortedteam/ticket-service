package com.sorted.rest.services.ticket.controller;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest.SortDirection;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.params.service.ParamService;
import com.sorted.rest.services.ticket.beans.*;
import com.sorted.rest.services.ticket.clients.ClientService;
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
import java.time.LocalDate;
import java.time.ZoneId;
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
	private ClientService clientService;

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
	public TicketBean createTicketsForIms(@Valid @RequestBody ImsCreateTicketRequest createTicketBean) {
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
		populateTicketDetailsAndInvokeCreateActions(requestTicket, requestTicketItems);
		requestTicket = ticketService.saveTicketWithItems(requestTicket, requestTicketItems);
		return convertTicketEntityIntoTicketBean(requestTicket, ticketCategoryEntities);
	}

	// todo: API not in use, to be integrated in subsequent releases
	@ApiOperation(value = "create tickets for partner app", nickname = "createTicketsForPartnerApp")
	@PostMapping(path = "/tickets/partner-app")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(propagation = Propagation.REQUIRED)
	public TicketBean createTicketsForPartnerApp(@Valid @RequestBody PartnerAppCreateTicketRequest createTicketBean) {
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
		populateTicketDetailsAndInvokeCreateActions(requestTicket, requestTicketItems);
		requestTicket = ticketService.saveTicketWithItems(requestTicket, requestTicketItems);
		return convertTicketEntityIntoTicketBean(requestTicket, ticketCategoryEntities);
	}

	// todo: API not in use, to be integrated in subsequent releases
	@ApiOperation(value = "create tickets for middle mile app", nickname = "createTicketsForMiddleMileApp")
	@PostMapping(path = "/tickets/middle-mile-app")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(propagation = Propagation.REQUIRED)
	public TicketBean createTicketsForMiddleMileApp(@Valid @RequestBody MiddleMileAppCreateTicketRequest createTicketBean) {
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
		populateTicketDetailsAndInvokeCreateActions(requestTicket, requestTicketItems);
		requestTicket = ticketService.saveTicketWithItems(requestTicket, requestTicketItems);
		return convertTicketEntityIntoTicketBean(requestTicket, ticketCategoryEntities);
	}

	private String getStoreCategoryForTicket(String storeId, String entityType) {
		List<String> storeCategoryForTicketParam = Arrays.stream(paramService.getParam("STORE_CATEGORY_FOR_TICKET", "Good|").split("\\|"))
				.collect(Collectors.toList());
		return clientService.getFilteredOrDefaultAudience(entityType, storeId,
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
					Set<String> skuCodes = dbEntity.getItems().stream()
							.filter(item -> !item.getStatus().equals(TicketStatus.CLOSED.toString()) && item.getResolutionDetails().getOrderDetails() != null)
							.map(item -> item.getResolutionDetails().getOrderDetails().getSkuCode())
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

	private String getTicketStatus(Integer isCategoryLeafTerminal, String categoryLeafDescription) {
		String status = null;
		if (isCategoryLeafTerminal == -1) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
					String.format("Can not save ticket with category : %s. Please retry with other category", categoryLeafDescription), null));
		} else if (isCategoryLeafTerminal == 0) {
			status = TicketStatus.DRAFT.toString();
		} else if (isCategoryLeafTerminal == 1) {
			status = TicketStatus.IN_PROGRESS.toString();
		}
		return status;
	}

	private void populateTicketDetailsAndInvokeCreateActions(TicketEntity requestTicket, List<TicketItemEntity> requestTicketItems) {
		try {
			ticketRequestUtils.populateTicketRequestAsPerCategoryRoot(requestTicket, requestTicketItems);
			ticketActionUtils.populateTicketResolutionAsPerCategoryRoot(requestTicket, requestTicketItems);
			Boolean hasNew = requestTicket.getHasNew(), hasUpdatedDraft = requestTicket.getHasUpdatedDraft();
			if (requestTicket.getId() == null) {
				requestTicket.setLastAddedOn(java.sql.Date.valueOf(LocalDate.now(ZoneId.of("Asia/Kolkata"))));
				requestTicket = ticketService.saveNewParentTicket(requestTicket);
				ticketActionUtils.addParentTicketHistory(requestTicket, hasNew, null, null);
			}

			if (hasNew) {
				for (TicketItemEntity item : requestTicketItems) {
					item.setTicket(requestTicket);
				}
				requestTicket.setLastAddedOn(java.sql.Date.valueOf(LocalDate.now(ZoneId.of("Asia/Kolkata"))));
				requestTicketItems = ticketItemService.saveAll(requestTicketItems);
			}
			for (TicketItemEntity item : requestTicketItems) {
				if (hasNew) {
					ticketActionUtils.invokeTicketCreateAction(item, requestTicket.getId());
				} else if (hasUpdatedDraft) {
					ticketActionUtils.invokeDraftTicketUpdateAction(item, requestTicket.getId());
				}

				if (item.getStatus().equals(TicketStatus.IN_PROGRESS.toString())) {
					ticketActionUtils.invokeTicketRaiseAction(item, requestTicket.getId());
				}
			}
		} catch (Exception e) {
			ticketRequestUtils.clearTicketRequest();
			throw e;
		}
		ticketRequestUtils.clearTicketRequest();
	}

	private TicketBean convertTicketEntityIntoTicketBean(TicketEntity ticket, List<TicketCategoryEntity> ticketCategoryEntities) {
		TicketBean ticketBean = getMapper().mapSrcToDest(ticket, TicketBean.newInstance());
		for (TicketItemBean itemBean : ticketBean.getItems()) {
			setTicketActionsAndCategory(itemBean, ticketBean.getCategoryRootId(), ticketCategoryEntities);
			if (ticketBean.getLastCreatedAt() == null || ticketBean.getLastCreatedAt().toInstant().isBefore(itemBean.getCreatedAt().toInstant())) {
				ticketBean.setLastCreatedAt(itemBean.getCreatedAt());
			}
		}
		return ticketBean;
	}

	private void setTicketActionsAndCategory(TicketItemBean itemBean, Integer categoryRootId, List<TicketCategoryEntity> ticketCategoryEntities) {
		itemBean.setUpdateActions(itemBean.getCategoryLeaf().getOnUpdateActions());
		itemBean.setCategory(ticketCategoryService.getRootToLeafPathUsingCategoryList(ticketCategoryEntities, categoryRootId, itemBean.getCategoryLeafId()));
	}

	@ApiOperation(value = "update ticket from draft for ims", nickname = "updateTicketFromDraftForIms")
	@PutMapping(path = "/tickets/ims/draft")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(propagation = Propagation.REQUIRED)
	public TicketBean updateTicketFromDraftForIms(@Valid @RequestBody UpdateTicketFromDraftBean updateTicketBean) {
		TicketEntity ticket = ticketService.findById(updateTicketBean.getId());
		if (ticket == null) {
			throw new ValidationException(
					ErrorBean.withError(Errors.NO_DATA_FOUND, String.format("No data found for ticket with id : %s", updateTicketBean.getItemId()), null));
		}
		Optional<TicketItemEntity> itemOptional = ticket.getItems().stream().filter(i -> Objects.equals(i.getId(), updateTicketBean.getItemId())).findFirst();
		if (itemOptional.isEmpty() || !itemOptional.get().getStatus().equals(TicketStatus.DRAFT.toString())) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND,
					String.format("No data found for ticket item  in draft status with id : %s", updateTicketBean.getId()), null));
		}
		TicketItemEntity item = itemOptional.get();
		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.findAllRecords();
		Map<Integer, TicketCategoryEntity> categoryMap = ticketCategoryEntities.stream()
				.collect(Collectors.toMap(TicketCategoryEntity::getId, Function.identity(), (o1, o2) -> o1, HashMap::new));
		if (updateTicketBean.getCategoryLeafId() != null && !updateTicketBean.getCategoryLeafId().equals(item.getCategoryLeafId())) {
			TicketCategoryEntity categoryLeaf = categoryMap.get(updateTicketBean.getCategoryLeafId());
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
		ticket.setHasUpdatedDraft(true);
		populateTicketDetailsAndInvokeCreateActions(ticket, Collections.singletonList(item));
		ticket = ticketService.saveTicketWithUpdatedItems(ticket);
		return convertTicketEntityIntoTicketBean(ticket, ticketCategoryEntities);
	}

	@ApiOperation(value = "update ticket for backoffice", nickname = "updateTicketForIms")
	@PutMapping(path = "/tickets/ims")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(propagation = Propagation.REQUIRED)
	public TicketBean updateTicketForIms(@Valid @RequestBody UpdateTicketBean updateTicketBean) {
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
		TicketItemEntity item = itemOptional.get();
		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.findAllRecords();

		if (updateTicketBean.getAttachments() != null && !updateTicketBean.getAttachments().isEmpty()) {
			item.setAttachments(Stream.concat(item.getAttachments().stream(), updateTicketBean.getAttachments().stream()).collect(Collectors.toList()));
		}

		populateTicketDetailsAndInvokeUpdateActions(ticket, item, updateTicketBean);
		ticket = ticketService.saveTicketWithUpdatedItems(ticket);
		return convertTicketEntityIntoTicketBean(ticket, ticketCategoryEntities);
	}

	private void populateTicketDetailsAndInvokeUpdateActions(TicketEntity requestTicket, TicketItemEntity requestItem, UpdateTicketBean updateTicketBean) {
		try {
			ticketRequestUtils.populateTicketRequestAsPerCategoryRoot(requestTicket, Collections.singletonList(requestItem));
			ticketActionUtils.populateTicketResolutionAsPerCategoryRoot(requestTicket, Collections.singletonList(requestItem));
			ticketActionUtils.invokeTicketUpdateAction(requestItem, requestTicket.getId(), updateTicketBean);
		} catch (Exception e) {
			ticketRequestUtils.clearTicketRequest();
			throw e;
		}
		ticketRequestUtils.clearTicketRequest();
	}

	@ApiOperation(value = "fetch paginated lists of tickets for ims", nickname = "fetchTicketsIms")
	@GetMapping(path = "/tickets/ims")
	public PageAndSortResult<TicketBean> fetchTicketsIms(@RequestParam(defaultValue = "1") Integer pageNo, @RequestParam(defaultValue = "25") Integer pageSize,
			@RequestParam(required = false) String sortBy, @RequestParam(required = false) PageAndSortRequest.SortDirection sortDirection,
			HttpServletRequest request) {
		Map<String, SortDirection> sort;
		if (sortBy != null) {
			sort = buildSortMap(sortBy, sortDirection);
		} else {
			sort = new LinkedHashMap<>();
			sort.put("lastAddedOn", PageAndSortRequest.SortDirection.DESC);
			sort.put("modifiedAt", PageAndSortRequest.SortDirection.DESC);
		}
		final Map<String, Object> params = getSearchParams(request, TicketEntity.class);
		PageAndSortResult<TicketEntity> tickets = ticketService.getAllTicketsPaginated(pageSize, pageNo, params, sort);
		PageAndSortResult<TicketBean> response = prepareResponsePageData(tickets, TicketBean.class);
		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.findAllRecords();
		for (TicketBean ticketBean : response.getData()) {
			for (TicketItemBean itemBean : ticketBean.getItems()) {
				setTicketActionsAndCategory(itemBean, ticketBean.getCategoryRootId(), ticketCategoryEntities);
			}
		}
		return response;
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
		if (!categoryMap.containsKey(TicketCategoryRoot.ORDER_ISSUE.toString())) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, "Order issue ticket category not found", null));
		}
		TicketCategoryEntity category = categoryMap.get(TicketCategoryRoot.ORDER_ISSUE.toString());
		Map<String, TicketItemEntity> ticketItemSkuMap = new HashMap<>();
		TicketEntity ticket = ticketService.findByReferenceIdAndCategoryRootId(request.getOrderId(), category.getId()).get(0);

		if (ticket == null) {
			ticket = createTicketForStoreReturn(request, category);
		} else {
			ticketItemSkuMap = ticket.getItems().stream()
					.filter(item -> !item.getStatus().equals(TicketStatus.CLOSED.toString()) && item.getDetails().getOrderDetails() != null && item.getDetails()
							.getOrderDetails().getSkuCode() != null)
					.collect(Collectors.toMap(item -> item.getDetails().getOrderDetails().getSkuCode(), Function.identity(), (o1, o2) -> o1, HashMap::new));
		}

		List<TicketItemEntity> insertTicketItems = new ArrayList<>();
		List<TicketItemEntity> updateTicketItems = new ArrayList<>();

		for (StoreReturnItemData requestItem : request.getItems()) {
			if (ticketItemSkuMap.containsKey(requestItem.getSkuCode())) {
				updateTicketItems.add(ticketItemSkuMap.get(requestItem.getSkuCode()));
			} else {
				TicketItemEntity ticketItem = createTicketItemForStoreReturn(requestItem, category);
				insertTicketItems.add(ticketItem);
			}
		}

		ticket.setHasNew(!insertTicketItems.isEmpty());
		if (ticket.getHasNew() || ticket.getId() == null) {
			populateTicketDetailsAndInvokeCreateActions(ticket, insertTicketItems);
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
		TicketDetailsBean ticketDetailsBean = TicketDetailsBean.newInstance();
		ticketDetailsBean.setDescription(TicketConstants.STORE_RETURN_TICKET_DESCRIPTION);
		OrderDetailsRequestBean orderDetailsRequestBean = OrderDetailsRequestBean.newInstance();
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

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}
}
