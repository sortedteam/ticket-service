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
import com.sorted.rest.services.ticket.constants.TicketConstants.*;
import com.sorted.rest.services.ticket.entity.TicketCategoryEntity;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.services.TicketCategoryService;
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
@Api(tags = "Ticket Services", description = "Manage Ticket related services.")
public class TicketController implements BaseController {

	AppLogger _LOGGER = LoggingManager.getLogger(TicketController.class);

	@Autowired
	private TicketService ticketService;

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

	@ApiOperation(value = "create tickets for partner app", nickname = "createTicketsForPartnerApp")
	@PostMapping(path = "/tickets/partner-app")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(propagation = Propagation.REQUIRED)
	public TicketBean createTicketsForPartnerApp(@Valid @RequestBody PartnerAppCreateTicketRequest createTicketBean) {
		TicketEntity ticket = getMapper().mapSrcToDest(createTicketBean, TicketEntity.newInstance());
		if (ticket == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No data given to create ticket", null));
		}
		String requesterEntityId = SessionUtils.getStoreId();
		if (requesterEntityId == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Store id not given", null));
		}
		String requesterEntityType = EntityType.STORE.toString();
		ticket.setRequesterEntityId(requesterEntityId);
		ticket.setRequesterEntityType(requesterEntityType);
		ticket.setRequesterEntityCategory(getStoreCategoryForTicket(requesterEntityId, requesterEntityType));

		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getVisibleTicketCategories();
		Map<Integer, TicketCategoryEntity> categoryMap = ticketCategoryEntities.stream()
				.collect(Collectors.toMap(TicketCategoryEntity::getId, Function.identity(), (o1, o2) -> o1, HashMap::new));
		validateAndSetTicketCategories(ticket, categoryMap);
		validateForDuplicateTickets(ticket);
		ticket.setHasNew(true);
		for (TicketItemEntity item : ticket.getItems()) {
			if (item.getCategoryLeaf().getIsTerminal() == -1) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
						String.format("Can not save ticket with category : %s. Please retry with other category", item.getCategoryLeaf().getDescription()),
						null));
			} else if (item.getCategoryLeaf().getIsTerminal() == 0) {
				item.setStatus(TicketStatus.DRAFT.toString());
			} else if (item.getCategoryLeaf().getIsTerminal() == 1) {
				item.setStatus(TicketStatus.IN_PROGRESS.toString());
			}
			item.setPlatform(TicketPlatform.PARTNER_APP.toString());
			item.setPriority(item.getCategoryLeaf().getPriority());
			item.setAssignedTeam(TicketResolutionTeam.CUSTOMERCARE.toString());
			item.setAssignedAt(new Date());
			item.setRemarks(TicketCreateActions.NEW_TICKET_CREATED.getRemarks());
			item.setIsNew(true);
		}
		populateTicketDetailsAndInvokeCreateActions(ticket);
		ticket = ticketService.save(ticket);
		return convertTicketEntityIntoTicketBean(ticket, ticketCategoryEntities);
	}

	private TicketBean convertTicketEntityIntoTicketBean(TicketEntity ticket, List<TicketCategoryEntity> ticketCategoryEntities) {
		TicketBean ticketBean = getMapper().mapSrcToDest(ticket, TicketBean.newInstance());
		for (TicketItemBean itemBean : ticketBean.getItems()) {
			setTicketActionsAndCategory(itemBean, ticketBean.getCategoryRootId(), ticketCategoryEntities);
		}
		return ticketBean;
	}

	private void setTicketActionsAndCategory(TicketItemBean itemBean, Integer categoryRootId, List<TicketCategoryEntity> ticketCategoryEntities) {
		itemBean.setUpdateActions(itemBean.getCategoryLeaf().getOnUpdateActions());
		itemBean.setCategory(ticketCategoryService.getRootToLeafPathUsingCategoryList(ticketCategoryEntities, categoryRootId, itemBean.getCategoryLeafId()));
	}

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

		TicketEntity ticket = getMapper().mapSrcToDest(createTicketBean, TicketEntity.newInstance());
		if (ticket == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No data given to create ticket", null));
		}
		String requesterEntityId = SessionUtils.getStoreId();
		if (requesterEntityId == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Store id not given", null));
		}

		if (ticket.getRequesterEntityType().equals(EntityType.STORE.toString())) {
			ticket.setRequesterEntityCategory(getStoreCategoryForTicket(ticket.getRequesterEntityId(), ticket.getRequesterEntityType()));
		}

		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getVisibleTicketCategories();
		Map<Integer, TicketCategoryEntity> categoryMap = ticketCategoryEntities.stream()
				.collect(Collectors.toMap(TicketCategoryEntity::getId, Function.identity(), (o1, o2) -> o1, HashMap::new));
		validateAndSetTicketCategories(ticket, categoryMap);
		validateForDuplicateTickets(ticket);
		ticket.setHasNew(true);
		for (TicketItemEntity item : ticket.getItems()) {
			if (item.getCategoryLeaf().getIsTerminal() != 1) {
				item.setStatus(TicketStatus.DRAFT.toString());
			} else {
				item.setStatus(TicketStatus.IN_PROGRESS.toString());
			}
			item.setPlatform(TicketPlatform.MIDDLE_MILE_APP.toString());
			item.setPriority(item.getCategoryLeaf().getPriority());
			item.setAssignedTeam(TicketResolutionTeam.CUSTOMERCARE.toString());
			item.setAssignedAt(new Date());
			item.setRemarks(TicketCreateActions.NEW_TICKET_CREATED.getRemarks());
			item.setIsNew(true);
		}
		populateTicketDetailsAndInvokeCreateActions(ticket);
		ticket = ticketService.save(ticket);
		return convertTicketEntityIntoTicketBean(ticket, ticketCategoryEntities);
	}

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

		TicketEntity ticket = getMapper().mapSrcToDest(createTicketBean, TicketEntity.newInstance());
		if (ticket == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No data given to create ticket", null));
		}
		String requesterEntityId = SessionUtils.getStoreId();
		if (requesterEntityId == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Store id not given", null));
		}

		if (ticket.getRequesterEntityType().equals(EntityType.STORE.toString())) {
			ticket.setRequesterEntityCategory(getStoreCategoryForTicket(ticket.getRequesterEntityId(), ticket.getRequesterEntityType()));
		}

		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.findAllRecords();
		Map<Integer, TicketCategoryEntity> categoryMap = ticketCategoryEntities.stream()
				.collect(Collectors.toMap(TicketCategoryEntity::getId, Function.identity(), (o1, o2) -> o1, HashMap::new));
		validateAndSetTicketCategories(ticket, categoryMap);
		validateForDuplicateTickets(ticket);
		ticket.setHasNew(true);
		for (TicketItemEntity item : ticket.getItems()) {
			if (item.getCategoryLeaf().getIsTerminal() != 1) {
				item.setStatus(TicketStatus.DRAFT.toString());
			} else {
				item.setStatus(TicketStatus.IN_PROGRESS.toString());
			}
			item.setPlatform(TicketPlatform.IMS.toString());
			item.setPriority(item.getCategoryLeaf().getPriority());
			item.setAssignedTeam(TicketResolutionTeam.CUSTOMERCARE.toString());
			item.setAssignedAt(new Date());
			item.setRemarks(TicketCreateActions.NEW_TICKET_CREATED.getRemarks());
			item.setIsNew(true);
		}
		populateTicketDetailsAndInvokeCreateActions(ticket);
		ticket = ticketService.save(ticket);
		return convertTicketEntityIntoTicketBean(ticket, ticketCategoryEntities);
	}

	private String getStoreCategoryForTicket(String storeId, String entityType) {
		List<String> storeCategoryForTicketParam = Arrays.stream(paramService.getParam("STORE_CATEGORY_FOR_TICKET", "Good|").split("\\|"))
				.collect(Collectors.toList());
		String requesterEntityCategory = clientService.getFilteredOrDefaultAudience(entityType, storeId,
				Arrays.stream(storeCategoryForTicketParam.get(1).split(",")).filter(s -> !StringUtils.isEmpty(s) && !StringUtils.isEmpty(s.trim()))
						.map(String::trim).distinct().collect(Collectors.toList()), storeCategoryForTicketParam.get(0));
		return requesterEntityCategory;
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

	private void populateTicketDetailsAndInvokeCreateActions(TicketEntity ticket) {
		try {
			ticketRequestUtils.populateTicketRequestAsPerCategoryRoot(ticket);
			ticketActionUtils.populateTicketResolutionAsPerCategoryRoot(ticket);
			if (ticket.getHasNew()) {
				ticket.setLastAddedOn(java.sql.Date.valueOf(LocalDate.now(ZoneId.of("Asia/Kolkata"))));
				ticket = ticketService.save(ticket);
			}
			for (TicketItemEntity item : ticket.getItems()) {
				if (item.getIsNew()) {
					ticketActionUtils.invokeTicketCreateAction(item, ticket.getId());
				} else if (item.getWasDraft()) {
					if (item.getCategoryLeaf().getIsTerminal() == -1) {
						throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
								String.format("Can not save ticket with category : %s. Please retry with other category",
										item.getCategoryLeaf().getDescription()), null));
					} else if (item.getCategoryLeaf().getIsTerminal() == 0) {
						item.setStatus(TicketStatus.DRAFT.toString());
					} else if (item.getCategoryLeaf().getIsTerminal() == 1) {
						item.setStatus(TicketStatus.IN_PROGRESS.toString());
					}
					ticketActionUtils.invokeDraftTicketUpdateAction(item, ticket.getId());
				}

				if (item.getStatus().equals(TicketStatus.IN_PROGRESS.toString())) {
					ticketActionUtils.invokeTicketRaiseAction(item, ticket.getId());
				}
			}
		} catch (Exception e) {
			ticketRequestUtils.clearTicketRequest();
			throw e;
		}
		ticketRequestUtils.clearTicketRequest();
	}

	private void validateForDuplicateTickets(TicketEntity ticket) {
		if (ticket.getRequesterEntityType().equals(EntityType.STORE.toString())) {
			if (ticket.getCategoryRoot().getLabel().equals(TicketCategoryRoot.ORDER_ISSUE.toString())) {
				if (StringUtils.isEmpty(ticket.getReferenceId())) {
					throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Ticket reference id can not be empty", "referenceIdNotFound"));
				}
				TicketEntity entity = ticketService.findByReferenceIdAndIsClosedInAndHasDraftInAndActive(ticket.getReferenceId(), List.of(false),
						List.of(true, false), 1).get(0);
				if (entity != null) {
					Set<String> skuCodes = entity.getItems().stream().filter(item -> item.getResolutionDetails().getOrderDetails() != null)
							.map(item -> item.getResolutionDetails().getOrderDetails().getSkuCode())
							.filter(skuCode -> !StringUtils.isEmpty(skuCode) && !StringUtils.isEmpty(skuCode.trim())).collect(Collectors.toSet());
					for (TicketItemEntity item : ticket.getItems()) {
						if (item.getDetails().getOrderDetails() != null && item.getDetails().getOrderDetails().getSkuCode() != null && skuCodes.contains(
								item.getDetails().getOrderDetails().getSkuCode())) {
							throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
									String.format("Ticket already exists for orderId : %s and skuCode : %s", ticket.getReferenceId(),
											item.getDetails().getOrderDetails().getSkuCode()), "duplicateTicket"));
						}
					}
					ticket.setId(entity.getId());
				}
			} else if (ticket.getCategoryRoot().getLabel().equals(TicketCategoryRoot.PAYMENT_ISSUE.toString())) {
				if (ticket.getReferenceId() != null) {
					TicketEntity entity = ticketService.findByReferenceIdAndIsClosedInAndHasDraftInAndActive(ticket.getReferenceId(), List.of(false),
							List.of(true, false), 1).get(0);
					if (entity != null && !entity.getItems().isEmpty()) {
						throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
								String.format("Ticket already exists for payment referenceId : %s", ticket.getReferenceId()), "duplicateTicket"));
					}
				}
			}
		}

	}

	@ApiOperation(value = "update ticket from draft for ims", nickname = "updateTicketFromDraftForIms")
	@PutMapping(path = "/tickets/draft/ims")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(propagation = Propagation.REQUIRED)
	public TicketBean updateTicketFromDraftForIms(@Valid @RequestBody UpdateTicketFromDraftBean updateTicketBean) {
		TicketEntity ticket = ticketService.findById(updateTicketBean.getTicketId());
		if (ticket == null) {
			throw new ValidationException(
					ErrorBean.withError(Errors.NO_DATA_FOUND, String.format("No data found for ticket with id : %s", updateTicketBean.getTicketId()), null));
		}
		Optional<TicketItemEntity> itemOptional = ticket.getItems().stream().filter(i -> i.getId() == updateTicketBean.getId()).findFirst();
		if (!itemOptional.isPresent() || !itemOptional.get().getStatus().equals(TicketStatus.DRAFT.toString())) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND,
					String.format("No data found for ticket item  in draft status with id : %s", updateTicketBean.getId()), null));
		}
		TicketItemEntity item = itemOptional.get();
		ticket.setItems(Collections.singletonList(item));
		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.findAllRecords();
		Map<Integer, TicketCategoryEntity> categoryMap = ticketCategoryEntities.stream()
				.collect(Collectors.toMap(TicketCategoryEntity::getId, Function.identity(), (o1, o2) -> o1, HashMap::new));
		if (updateTicketBean.getCategoryLeafId() != null && updateTicketBean.getCategoryLeafId() != item.getCategoryLeafId()) {
			TicketCategoryEntity categoryLeaf = categoryMap.get(updateTicketBean.getCategoryLeafId());
			if (categoryLeaf == null) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
						String.format("No data found for ticket category with id : %s", updateTicketBean.getCategoryLeafId()), null));
			}
			item.setCategoryLeafId(updateTicketBean.getCategoryLeafId());
			item.setCategoryLeaf(categoryLeaf);
			item.setPriority(item.getCategoryLeaf().getPriority());
		}
		if (updateTicketBean.getAttachments() != null && !updateTicketBean.getAttachments().isEmpty()) {
			item.setAttachments(Stream.concat(item.getAttachments().stream(), updateTicketBean.getAttachments().stream()).collect(Collectors.toList()));
		}
		if (updateTicketBean.getDetails() != null) {
			item.setDetails(getMapper().mapSrcToDest(updateTicketBean.getDetails(), TicketDetailsBean.newInstance()));
		}
		item.setWasDraft(true);
		populateTicketDetailsAndInvokeCreateActions(ticket);
		ticket.setItems(Collections.singletonList(item));
		ticket = ticketService.save(ticket);
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
		Optional<TicketItemEntity> itemOptional = ticket.getItems().stream().filter(i -> i.getId() == updateTicketBean.getId()).findFirst();
		if (!itemOptional.isPresent()) {
			throw new ValidationException(
					ErrorBean.withError(Errors.NO_DATA_FOUND, String.format("No data found for ticket item with id : %s", updateTicketBean.getId()), null));
		}
		TicketItemEntity item = itemOptional.get();
		ticket.setItems(Collections.singletonList(item));
		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.findAllRecords();
		Map<Integer, TicketCategoryEntity> categoryMap = ticketCategoryEntities.stream()
				.collect(Collectors.toMap(TicketCategoryEntity::getId, Function.identity(), (o1, o2) -> o1, HashMap::new));
		if (updateTicketBean.getCategoryLeafId() != null && updateTicketBean.getCategoryLeafId() != item.getCategoryLeafId()) {
			TicketCategoryEntity ticketCategoryLeaf = categoryMap.get(updateTicketBean.getCategoryLeafId());
			if (ticketCategoryLeaf == null) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
						String.format("No data found for ticket category with id : %s", updateTicketBean.getCategoryLeafId()), null));
			}
			item.setCategoryLeafId(updateTicketBean.getCategoryLeafId());
			item.setCategoryLeaf(ticketCategoryLeaf);
			item.setPriority(item.getCategoryLeaf().getPriority());
		}
		if (updateTicketBean.getAttachments() != null && !updateTicketBean.getAttachments().isEmpty()) {
			item.setAttachments(Stream.concat(item.getAttachments().stream(), updateTicketBean.getAttachments().stream()).collect(Collectors.toList()));
		}

		ticketActionUtils.invokeTicketUpdateAction(item, ticket.getId(), updateTicketBean);
		ticket.setItems(Collections.singletonList(item));
		ticket = ticketService.save(ticket);
		return convertTicketEntityIntoTicketBean(ticket, ticketCategoryEntities);
	}

	@ApiOperation(value = "fetch paginated lists of tickets for ims", nickname = "fetchTicketsIms")
	@GetMapping(path = "/tickets/ims")
	public PageAndSortResult<TicketBean> fetchTicketsIms(@RequestParam(defaultValue = "1") Integer pageNo, @RequestParam(defaultValue = "25") Integer pageSize,
			@RequestParam(required = false) String sortBy, @RequestParam(required = false) PageAndSortRequest.SortDirection sortDirection,
			HttpServletRequest request) {
		Map<String, SortDirection> sort = null;
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

	/*
	@ApiOperation(value = "create or update tickets for store return", nickname = "createOrUpdateTicketsForStoreReturn")
	@PostMapping(path = "/tickets/store-return")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(propagation = Propagation.REQUIRED)
	public List<TicketBean> createOrUpdateTicketsForStoreReturn(@RequestBody StoreReturnTicketRequest storeReturnTicketRequest) {
		List<TicketEntity> tickets = getMapper().mapAsList(storeReturnTicketRequest.getStoreReturnItemDataList(), TicketEntity.class);
		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getVisibleTicketCategories();
		Map<Integer, TicketCategoryEntity> categoryMap = ticketCategoryEntities.stream()
				.collect(Collectors.toMap(TicketCategoryEntity::getId, Function.identity(), (o1, o2) -> o1, HashMap::new));
		validateCommonDetailsForPartnerAppRequest(tickets);
		validateAndSetTicketCategories(tickets, categoryMap);
		validateForDuplicateTickets(tickets);
		for (TicketEntity ticket : tickets) {
			if (ticket.getCategoryLeaf().getIsTerminal() != 1) {
				ticket.setStatus(TicketStatus.DRAFT.toString());
			} else {
				ticket.setStatus(TicketStatus.IN_PROGRESS.toString());
			}
			ticket.setPlatform(TicketPlatform.PARTNER_APP.toString());
			ticket.setPriority(ticket.getCategoryLeaf().getPriority());
			ticket.setRemarks(TicketConstants.NEW_TICKET_CREATED_REMARKS);
			ticket.setAssignedTeam(TicketResolutionTeam.CUSTOMERCARE.toString());
			ticket.setAssignedAt(new Date());
		}
		tickets = ticketService.saveAll(tickets);
		populateTicketDetailsAndInvokeActions(tickets);
		tickets = ticketService.saveAll(tickets);
		return convertTicketEntitiesIntoBeans(tickets, ticketCategoryEntities);
	}


	@ApiOperation(value = "fetch paginated lists of tickets", nickname = "fetchTicketsGroupedByReferenceIdListView")
	@GetMapping(path = "/tickets/grouped")
	public PageAndSortResult<TicketBean> fetchTicketsGroupedByReferenceIdListView(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "25") Integer pageSize, @RequestParam(required = false) String sortBy,
			@RequestParam(required = false) PageAndSortRequest.SortDirection sortDirection, HttpServletRequest request,
			@RequestParam(required = false) List<String> statusIn, @RequestParam(required = false) List<Integer> categoryRootIn) {
		Map<String, SortDirection> sort = null;
		if (sortBy != null) {
			sort = buildSortMap(sortBy, sortDirection);
		} else {
			sort = new LinkedHashMap<>();
			sort.put("modifiedAt", PageAndSortRequest.SortDirection.DESC);
		}
		final Map<String, Object> params = getSearchParams(request, TicketEntity.class);
		//		if (statusIn != null && !statusIn.isEmpty()) {
		//			params.put("status", statusIn);
		//		}
		//		if (categoryRootIn != null && !categoryRootIn.isEmpty()) {
		//			params.put("categoryRootId", categoryRootIn);
		//		}
		//		if (params.containsKey("statusIn")) {
		//			params.remove("statusIn");
		//		}
		//		if (params.containsKey("categoryRootIn")) {
		//			params.remove("categoryRootIn");
		//		}
		PageAndSortResult<String> tickets = ticketService.getAllGroupedTicketsPaginated(pageSize, pageNo, params, sort);
		PageAndSortResult<TicketBean> response = prepareResponsePageData(tickets, TicketBean.class);
		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.findAllRecords();
		for (TicketBean ticketBean : response.getData()) {
			setTicketActionsAndCategory(ticketBean, ticketCategoryEntities);
		}
		return response;
	}

	 */

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
