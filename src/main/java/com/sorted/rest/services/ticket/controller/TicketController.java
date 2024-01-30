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
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.common.utils.ParamsUtils;
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

import javax.validation.Valid;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
		TicketEntity requestTicket = getMapper().mapSrcToDest(createTicketBean, TicketEntity.newInstance());
		if (requestTicket == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No data given to create ticket", null));
		}

		if (requestTicket.getRequesterEntityType().equals(EntityType.STORE)) {
			requestTicket.setRequesterEntityCategory(getStoreCategoryForTicket(requestTicket.getRequesterEntityId(), requestTicket.getRequesterEntityType()));
		} else if (requestTicket.getRequesterEntityType().equals(EntityType.USER)) {
			requestTicket.setRequesterEntityCategory(TicketConstants.DEFAULT_USER_CATEGORY);
		}

		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getAllTicketCategories(createTicketBean.getRequesterEntityType());
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

		EntityType requesterEntityType = EntityType.STORE;
		requestTicket.setRequesterEntityId(requesterEntityId);
		requestTicket.setRequesterEntityType(requesterEntityType);
		requestTicket.setRequesterEntityCategory(getStoreCategoryForTicket(requesterEntityId, requesterEntityType));

		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getAllTicketCategories(requesterEntityType);
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

/*
	// todo: API not in use, to be integrated in subsequent releases
	@ApiOperation(value = "create tickets for middle mile app", nickname = "createTicketsForMiddleMileApp")
	@PostMapping(path = "/tickets/middle-mile-app")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(propagation = Propagation.REQUIRED)
	public void createTicketsForMiddleMileApp(@Valid @RequestBody MiddleMileAppCreateTicketRequest createTicketBean) {
		_LOGGER.info(String.format("createTicketsForMiddleMileApp:: request %s", createTicketBean));

		TicketEntity requestTicket = getMapper().mapSrcToDest(createTicketBean, TicketEntity.newInstance());
		if (requestTicket == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No data given to create ticket", null));
		}

		if (requestTicket.getRequesterEntityType().equals(EntityType.STORE)) {
			requestTicket.setRequesterEntityCategory(getStoreCategoryForTicket(requestTicket.getRequesterEntityId(), requestTicket.getRequesterEntityType()));
		}

		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getAllTicketCategories(createTicketBean.getRequesterEntityType());
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
*/

	private String getStoreCategoryForTicket(String storeId, EntityType entityType) {
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
		if (requestTicket.getRequesterEntityType().equals(EntityType.STORE)) {
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
		} else if (requestTicket.getRequesterEntityType().equals(EntityType.USER)) {
			if (requestTicket.getCategoryRoot().getLabel().equals(TicketCategoryRoot.CONSUMER_ORDER_ISSUE.toString())) {
				if (StringUtils.isEmpty(requestTicket.getReferenceId())) {
					throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Ticket reference id can not be empty", "referenceIdNotFound"));
				}
				dbEntity = ticketService.findByReferenceIdAndCategoryRootId(requestTicket.getReferenceId(), requestTicket.getCategoryRootId()).get(0);
				if (dbEntity != null) {
					Set<String> skuCodes = dbEntity.getItems().stream().filter(item -> !item.getStatus().equals(TicketStatus.CLOSED) && !item.getStatus()
									.equals(TicketStatus.CANCELLED) && item.getDetails().getConsumerOrderDetails() != null)
							.map(item -> item.getDetails().getConsumerOrderDetails().getSkuCode())
							.filter(skuCode -> !StringUtils.isEmpty(skuCode) && !StringUtils.isEmpty(skuCode.trim())).collect(Collectors.toSet());
					for (TicketItemEntity item : requestTicket.getItems()) {
						if (item.getDetails().getConsumerOrderDetails() != null && item.getDetails().getConsumerOrderDetails()
								.getSkuCode() != null && skuCodes.contains(item.getDetails().getConsumerOrderDetails().getSkuCode())) {
							throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
									String.format("Ticket already exists for orderId : %s and skuCode : %s", requestTicket.getReferenceId(),
											item.getDetails().getConsumerOrderDetails().getSkuCode()), "duplicateTicket"));
						}
					}
				}
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
			ticketActionUtils.populateTicketDetailsAsPerCategoryRoot(requestTicket, requestTicketItems);
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
					ticketActionUtils.invokeTicketRaiseAction(item, requestTicket);
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
		itemBean.setCategory(
				ticketCategoryService.getRootToLeafPathUsingCategoryList(ticketCategoryEntities, categoryRootId, itemBean.getCategoryLeaf().getId()));
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
		return getItemBean(ticket, updateTicketBean.getItemId());
	}

	@ApiOperation(value = "update ticket for backoffice", nickname = "updateTicketForIms")
	@PutMapping(path = "/tickets/ims")
	@ResponseStatus(HttpStatus.OK)
	public TicketItemBean updateTicketForIms(@Valid @RequestBody UpdateTicketBean updateTicketBean) {
		TicketEntity ticket = updateImsTicket(updateTicketBean);
		//checkTicketAndGiveTargetCashback(ticket);
		return getItemBean(ticket, updateTicketBean.getItemId());
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public TicketEntity updateImsTicket(UpdateTicketBean updateTicketBean) {
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
		return ticketService.saveTicketWithUpdatedItems(ticket);
	}

	private void checkTicketAndGiveTargetCashback(TicketEntity ticket) {
		try {
			if (ticket.getPendingCount() == 0 && ticket.getMetadata().getOrderDetails() != null && ticket.getMetadata().getOrderDetails()
					.getDeliveryDate() != null && checkCashbackDateConditions(ticket.getMetadata().getOrderDetails().getDeliveryDate())) {
				this.ticketClientService.giveTargetCashbackForStoreIdAndDate(ticket.getRequesterEntityId(),
						ticket.getMetadata().getOrderDetails().getDeliveryDate());
			}
		} catch (Exception e) {
			_LOGGER.info(String.format("error while running cashback cron : %s", e));
		}
	}

	private boolean checkCashbackDateConditions(Date orderDeliveryDate) {
		LocalDateTime currentTime = LocalDateTime.now().plusHours(5).plusMinutes(30);
		LocalDateTime deliveryDate = orderDeliveryDate.toInstant().atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
		if ((currentTime.toLocalDate().isEqual(deliveryDate.toLocalDate()) && LocalTime.now(ZoneId.of("Asia/Kolkata"))
				.isAfter(LocalTime.of(ParamsUtils.getIntegerParam("TARGET_CASHBACK_TIME", 20), 0)))) {
			return true;
		}
		return deliveryDate.toLocalDate().isBefore(currentTime.toLocalDate());
	}

	private TicketItemBean getItemBean(TicketEntity ticket, Long itemId) {
		Optional<TicketItemEntity> itemOptional = ticket.getItems().stream().filter(i -> Objects.equals(i.getId(), itemId)).findFirst();
		if (itemOptional.isPresent()) {
			TicketItemBean itemBean = getMapper().mapSrcToDest(itemOptional.get(), TicketItemBean.newInstance());
			List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getAllTicketCategoriesWithoutActive(ticket.getRequesterEntityType());
			setTicketActionsAndCategory(itemBean, ticket.getCategoryRootId(), ticketCategoryEntities);
			return itemBean;
		} else {
			throw new ValidationException(
					ErrorBean.withError(Errors.NO_DATA_FOUND, String.format("No data found after updating ticket with id : %s", itemId), null));
		}
	}

	private void populateTicketDetailsAndInvokeUpdateActions(TicketEntity requestTicket, TicketItemEntity requestItem, UpdateTicketBean updateTicketBean) {
		try {
			ticketRequestUtils.populateTicketRequestAsPerCategoryRoot(requestTicket, Collections.singletonList(requestItem));
			ticketActionUtils.invokeTicketUpdateAction(requestItem, requestTicket, updateTicketBean);
			ticketRequestUtils.clearTicketRequest();
		} catch (Exception e) {
			ticketRequestUtils.clearTicketRequest();
			throw e;
		}
	}

	@ApiOperation(value = "fetch paginated lists of tickets for ims", nickname = "fetchTicketsIms")
	@GetMapping(path = "/tickets/ims")
	public PageAndSortResult<TicketListViewBean> fetchTicketsIms(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "25") Integer pageSize, @RequestParam(required = false) String sortBy,
			@RequestParam(required = false) PageAndSortRequest.SortDirection sortDirection, @RequestParam(required = false) String requesterEntityId,
			@RequestParam(required = false) String requesterEntityCategory, @RequestParam Boolean orderRelated,
			@RequestParam(required = false) String lastAddedOn, @RequestParam(required = false) Boolean hasDraft,
			@RequestParam(required = false) Boolean hasPending, @RequestParam(required = false) Boolean hasClosed,
			@RequestParam(required = false) Integer categoryLeafParentId, @RequestParam(defaultValue = "false") Boolean showOnlyMappedStores,
			@RequestParam(defaultValue = "STORE") EntityType requesterEntityType) throws ParseException {
		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getAllTicketCategoriesWithoutActive(requesterEntityType);

		Map<String, SortDirection> sort;
		if (sortBy != null) {
			sort = buildSortMap(sortBy, sortDirection);
		} else {
			sort = new LinkedHashMap<>();
			sort.put("lastAddedAt", PageAndSortRequest.SortDirection.DESC);
		}
		List requesterEntityIds = new ArrayList();
		if (requesterEntityId != null) {
			requesterEntityIds.add(requesterEntityId);
		}
		if (requesterEntityType.equals(EntityType.STORE) && showOnlyMappedStores != null && showOnlyMappedStores) {
			Set<String> mappedStores = ticketClientService.getMappedStores(SessionUtils.getAuthUserId());
			if (mappedStores.isEmpty() || (requesterEntityId != null && !mappedStores.contains(requesterEntityId))) {
				throw new ValidationException(
						new ErrorBean(Errors.INVALID_REQUEST, "Store(s) not mapped to the user, please try disabling show only mapped stores filter"));
			}
			if (requesterEntityId == null) {
				requesterEntityIds.addAll(mappedStores);
			}
		}

		PageAndSortResult<TicketListViewBean> response;
		if (categoryLeafParentId != null) {
			List<TicketListViewBean> tickets = getMapper().mapAsList(
					ticketService.getCategoryLeafFilteredTickets(requesterEntityType, requesterEntityId, requesterEntityIds, requesterEntityCategory,
							orderRelated, lastAddedOn, hasDraft, hasPending, hasClosed, ticketCategoryEntities, categoryLeafParentId),
					TicketListViewBean.class);
			response = new PageAndSortResult<>(1, tickets.size(), 1, tickets.size(), tickets);
		} else {
			final Map<String, Object> filters = ticketService.getFilters(requesterEntityType, requesterEntityId, requesterEntityIds, requesterEntityCategory,
					orderRelated, lastAddedOn, hasDraft, hasPending, hasClosed, ticketCategoryEntities);
			PageAndSortResult<TicketEntity> tickets = ticketService.getAllTicketsPaginated(pageSize, pageNo, filters, sort);
			response = prepareResponsePageData(tickets, TicketListViewBean.class);
		}

		if (orderRelated) {
			updateOrderDetailsFromClient(response.getData());
		}
		return response;
	}

	private <T extends TicketListViewBean> void updateOrderDetailsFromClient(List<T> ticketBeans) {
		if (CollectionUtils.isEmpty(ticketBeans))
			return;
		if (ticketBeans.get(0).getRequesterEntityType().equals(EntityType.STORE)) {
			Set<String> displayOrderIds = ticketBeans.stream()
					.filter(ticket -> ticket.getCategoryRoot().getLabel().equals(TicketCategoryRoot.ORDER_ISSUE.toString()) && ticket.getMetadata()
							.getOrderDetails() != null && !StringUtils.isEmpty(ticket.getMetadata().getOrderDetails().getDisplayOrderId()))
					.map(ticket -> ticket.getMetadata().getOrderDetails().getDisplayOrderId()).collect(Collectors.toSet());
			Map<String, FranchiseOrderListBean> ordersDisplayIdMap = !displayOrderIds.isEmpty() ?
					ticketClientService.getFranchiseOrderByDisplayIds(displayOrderIds).stream()
							.collect(Collectors.toMap(FranchiseOrderListBean::getDisplayOrderId, Function.identity())) :
					new HashMap<>();

			for (T ticketBean : ticketBeans) {
				if (ticketBean.getMetadata().getOrderDetails() != null && ticketBean.getMetadata().getOrderDetails()
						.getDisplayOrderId() != null && ordersDisplayIdMap.containsKey(ticketBean.getMetadata().getOrderDetails().getDisplayOrderId())) {
					FranchiseOrderListBean orderListBean = ordersDisplayIdMap.get(ticketBean.getMetadata().getOrderDetails().getDisplayOrderId());
					ticketBean.getMetadata().getOrderDetails().setOrderStatus(orderListBean.getStatus());
				}
			}
		} else if (ticketBeans.get(0).getRequesterEntityType().equals(EntityType.USER)) {
			Set<String> displayOrderIds = ticketBeans.stream()
					.filter(ticket -> ticket.getCategoryRoot().getLabel().equals(TicketCategoryRoot.CONSUMER_ORDER_ISSUE.toString()) && ticket.getMetadata()
							.getConsumerOrderDetails() != null && !StringUtils.isEmpty(ticket.getMetadata().getConsumerOrderDetails().getDisplayOrderId()))
					.map(ticket -> ticket.getMetadata().getConsumerOrderDetails().getDisplayOrderId()).collect(Collectors.toSet());
			Map<String, ConsumerOrderListBean> ordersDisplayIdMap = !displayOrderIds.isEmpty() ?
					ticketClientService.getConsumerOrderByDisplayIds(displayOrderIds).stream()
							.collect(Collectors.toMap(ConsumerOrderListBean::getDisplayOrderId, Function.identity())) :
					new HashMap<>();

			for (T ticketBean : ticketBeans) {
				if (ticketBean.getMetadata().getConsumerOrderDetails() != null && ticketBean.getMetadata().getConsumerOrderDetails()
						.getDisplayOrderId() != null && ordersDisplayIdMap.containsKey(
						ticketBean.getMetadata().getConsumerOrderDetails().getDisplayOrderId())) {
					ConsumerOrderListBean orderListBean = ordersDisplayIdMap.get(ticketBean.getMetadata().getConsumerOrderDetails().getDisplayOrderId());
					ticketBean.getMetadata().getConsumerOrderDetails().setOrderStatus(orderListBean.getStatus());
				}
			}
		}
	}

	private void filterTicketOnShowStatus(Boolean show, TicketStatus status, List<TicketBean> ticketBeans) {
		List<TicketBean> removeList = new ArrayList<>();
		for (TicketBean ticketBean : ticketBeans) {
			List<TicketItemBean> filteredItems = ticketBean.getItems().stream().filter(item -> !(show ^ item.getStatus().equals(status.toString())))
					.collect(Collectors.toList());
			if (filteredItems.isEmpty()) {
				removeList.add(ticketBean);
			} else {
				ticketBean.setItems(filteredItems);
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

		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getAllTicketCategories(EntityType.STORE);
		Map<String, TicketCategoryEntity> categoryMap = ticketCategoryEntities.stream()
				.collect(Collectors.toMap(TicketCategoryEntity::getLabel, Function.identity(), (o1, o2) -> o1, HashMap::new));
		if (!categoryMap.containsKey(TicketCategoryRoot.ORDER_ISSUE.toString()) || !categoryMap.containsKey(
				TicketConstants.STORE_RETURN_TICKET_CATEGORY_LEAF_LABEL) || (request.getTicketCategoryLabel() != null && !categoryMap.containsKey(
				request.getTicketCategoryLabel()))) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, "Relevant order issue ticket category not found", null));
		}
		TicketCategoryEntity categoryRoot = categoryMap.get(TicketCategoryRoot.ORDER_ISSUE.toString());
		TicketCategoryEntity categoryLeaf = categoryMap.get(TicketConstants.STORE_RETURN_TICKET_CATEGORY_LEAF_LABEL);
		TicketCategoryEntity fullSrCategoryLeaf = request.getTicketCategoryLabel() != null ? categoryMap.get(request.getTicketCategoryLabel()) : null;
		Map<String, List<TicketItemEntity>> ticketItemSkuMap = new HashMap<>();

		TicketEntity ticket = ticketService.findByReferenceIdAndCategoryRootId(request.getOrderId(), categoryRoot.getId()).get(0);

		if (ticket == null) {
			ticket = createTicketForStoreReturn(request, categoryRoot);
		} else {
			ticketItemSkuMap = ticket.getItems().stream()
					.filter(item -> item.getDetails().getOrderDetails() != null && item.getDetails().getOrderDetails().getSkuCode() != null).collect(
							Collectors.groupingBy(item -> item.getDetails().getOrderDetails().getSkuCode(),
									Collectors.mapping(Function.identity(), Collectors.toList())));
		}

		List<TicketItemEntity> insertTicketItems = new ArrayList<>();
		List<TicketItemEntity> updateTicketItems = new ArrayList<>();

		for (StoreReturnItemData requestItem : request.getItems()) {
			if (ticketItemSkuMap.containsKey(requestItem.getSkuCode())) {
				updateTicketItems.addAll(ticketItemSkuMap.get(requestItem.getSkuCode()));
			} else if (request.getIsFullSrReturn() == null || request.getIsFullSrReturn() == 0) {
				TicketItemEntity ticketItem = createTicketItemForStoreReturn(requestItem, categoryLeaf);
				insertTicketItems.add(ticketItem);
			}
		}

		TicketItemEntity fullSrReturnTicketItem = null;
		StoreReturnItemData fullSrItemData = null;
		if (request.getIsFullSrReturn() != null && request.getIsFullSrReturn() == 1) {
			fullSrItemData = new StoreReturnItemData();
			fullSrItemData.setQaResult(request.getQaResult());
			fullSrItemData.setRemarks(request.getRemarks());
			fullSrReturnTicketItem = createTicketItemForFullSrStoreReturn(request, fullSrCategoryLeaf);
			insertTicketItems.add(fullSrReturnTicketItem);
		}

		ticket.setHasNew(!insertTicketItems.isEmpty());
		if (ticket.getHasNew() || ticket.getId() == null) {
			populateTicketDetailsAndInvokeCreateOrUpdateActions(ticket, insertTicketItems);
			ticket.addTicketItems(insertTicketItems);
		}

		ticketItemSkuMap = Stream.concat(updateTicketItems.stream(), insertTicketItems.stream())
				.filter(item -> item.getDetails().getOrderDetails() != null && item.getDetails().getOrderDetails().getSkuCode() != null).collect(
						Collectors.groupingBy(item -> item.getDetails().getOrderDetails().getSkuCode(),
								Collectors.mapping(Function.identity(), Collectors.toList())));

		for (StoreReturnItemData requestItem : request.getItems()) {
			if (ticketItemSkuMap.containsKey(requestItem.getSkuCode())) {
				for (TicketItemEntity item : ticketItemSkuMap.get(requestItem.getSkuCode())) {
					ticketActionUtils.invokeUpdateStoreReturnInfoAction(item, ticket.getId(), requestItem);
				}
			}
		}
		if (fullSrItemData != null && fullSrReturnTicketItem != null) {
			ticketActionUtils.invokeUpdateStoreReturnInfoAction(fullSrReturnTicketItem, ticket.getId(), fullSrItemData);
		}
		ticketService.saveTicketWithUpdatedItems(ticket);
	}

	private TicketEntity createTicketForStoreReturn(StoreReturnTicketRequest request, TicketCategoryEntity category) {
		TicketEntity ticket = TicketEntity.newInstance();
		ticket.setReferenceId(request.getOrderId());
		ticket.setRequesterEntityId(request.getStoreId());
		ticket.setRequesterEntityType(EntityType.STORE);
		ticket.setRequesterEntityCategory(getStoreCategoryForTicket(request.getStoreId(), EntityType.STORE));
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
		TicketItemEntity ticketItem = createTicketItemForStoreReturn(category, ticketDetailsBean);
		return ticketItem;
	}

	private TicketItemEntity createTicketItemForStoreReturn(TicketCategoryEntity category, ResolutionDetailsBean ticketDetailsBean) {
		TicketItemEntity ticketItem = TicketItemEntity.newInstance();
		ticketItem.setDetails(ticketDetailsBean);
		ticketItem.setCategoryLeaf(category);
		ticketItem.setStatus(getTicketStatus(ticketItem.getCategoryLeaf().getIsTerminal(), ticketItem.getCategoryLeaf().getDescription()));
		ticketItem.setPlatform(TicketPlatform.STORE_RETURN.toString());
		ticketItem.setPriority(ticketItem.getCategoryLeaf().getPriority());
		ticketItem.setAssignedTeam(TicketResolutionTeam.CUSTOMERCARE.toString());
		ticketItem.setAssignedAt(new Date());
		return ticketItem;
	}

	private TicketItemEntity createTicketItemForFullSrStoreReturn(StoreReturnTicketRequest request, TicketCategoryEntity category) {
		ResolutionDetailsBean ticketDetailsBean = ResolutionDetailsBean.newInstance();
		ticketDetailsBean.setDescription(TicketConstants.STORE_RETURN_TICKET_DESCRIPTION);
		OrderItemDetailsBean orderDetailsRequestBean = OrderItemDetailsBean.newInstance();
		ticketDetailsBean.setOrderDetails(orderDetailsRequestBean);
		return createTicketItemForStoreReturn(category, ticketDetailsBean);
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
		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getAllTicketCategoriesWithoutActive(ticket.getRequesterEntityType());
		TicketBean ticketBean = getMapper().mapSrcToDest(ticket, TicketBean.newInstance());
		List<TicketBean> ticketBeans = new ArrayList<>();
		ticketBeans.add(ticketBean);
		if (showOnlyDraft) {
			filterTicketOnShowStatus(true, TicketStatus.DRAFT, ticketBeans);
		}
		updateOrderDetailsFromClient(ticketBeans);
		for (TicketItemBean itemBean : ticketBean.getItems()) {
			setTicketActionsAndCategory(itemBean, ticketBean.getCategoryRootId(), ticketCategoryEntities);
			setTicketCategoryDesc(itemBean);
		}
		return ResponseEntity.ok(ticketBean);
	}

	@ApiOperation(value = "fetch tickets for partner app", nickname = "fetchTicketsForPartnerApp")
	@GetMapping(path = "/tickets/partner-app")
	public ResponseEntity<List<TicketBean>> fetchTicketsForPartnerApp() {
		String requesterEntityId = SessionUtils.getStoreId();
		if (requesterEntityId == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Store id not given", null));
		}
		EntityType requesterEntityType = EntityType.STORE;
		Integer sinceDays = ParamsUtils.getIntegerParam("PARTNER_APP_TICKET_LIST_DAYS", 10);
		Map<String, Object> filters = new HashMap<>();
		filters.put("requesterEntityId", requesterEntityId);
		filters.put("requesterEntityType", requesterEntityType);
		Date fromDate = DateUtils.addDays(new Date(), -1 * sinceDays);
		filters.put("fromDate", new FilterCriteria("lastAddedAt", fromDate, Operation.GTE));
		Map<String, PageAndSortRequest.SortDirection> sort = new LinkedHashMap<>();
		sort.put("lastAddedAt", PageAndSortRequest.SortDirection.DESC);

		List<TicketEntity> tickets = ticketService.findAllRecords(filters, sort);
		if (tickets == null || tickets.isEmpty()) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND,
					String.format("No tickets found for storeId : %s for the last : %d days", requesterEntityId, sinceDays), null));
		}

		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getAllTicketCategoriesWithoutActive(requesterEntityType);
		List<TicketBean> ticketBeans = getMapper().mapAsList(tickets, TicketBean.class);

		filterTicketOnShowStatus(false, TicketStatus.DRAFT, ticketBeans);
		filterTicketOnShowStatus(false, TicketStatus.CANCELLED, ticketBeans);
		updateOrderDetailsFromClient(ticketBeans);
		for (TicketBean ticketBean : ticketBeans) {
			for (TicketItemBean itemBean : ticketBean.getItems()) {
				setTicketActionsAndCategory(itemBean, ticketBean.getCategoryRootId(), ticketCategoryEntities);
				setTicketCategoryDesc(itemBean);
			}
		}
		return ResponseEntity.ok(ticketBeans);
	}

	private void setTicketCategoryDesc(TicketItemBean itemBean) {
		itemBean.setLeafParentCategoryDesc(itemBean.getCategoryLeaf().getDescription());
		if (itemBean.getCategoryLeaf().getParentId() != null) {
			TicketCategoryEntity categoryLeafPrevious = ticketCategoryService.findRecordById(itemBean.getCategoryLeaf().getParentId());
			if (categoryLeafPrevious != null) {
				itemBean.setLeafCategoryDesc(itemBean.getCategoryLeaf().getDescription());
				itemBean.setLeafParentCategoryDesc(categoryLeafPrevious.getDescription());
			}
		}
	}

	@ApiOperation(value = "fetch pending order tickets", nickname = "fetchPendingOrderTickets")
	@PostMapping(path = "/tickets/internal/pending-ticket-orders")
	public ResponseEntity<PendingOrderRefundTicketsResponse> fetchTicketsForPartnerApp(
			@Valid @RequestBody PendingOrderRefundTicketsRequest pendingOrderRefundTicketsRequest) {
		List<String> pendingRefundOrderIds = ticketService.getPendingStoreRefundTickets(pendingOrderRefundTicketsRequest.getOrderIds());
		PendingOrderRefundTicketsResponse response = new PendingOrderRefundTicketsResponse();
		response.setOrderIds(pendingRefundOrderIds);
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "fetch custom store order tickets for ims", nickname = "fetchCustomOrderTicketsForIms")
	@GetMapping(path = "/tickets/ims/orders")
	public ResponseEntity<List<TicketBean>> fetchCustomOrderTicketsForIms(@RequestParam java.sql.Date createdFrom, @RequestParam java.sql.Date createdTo,
			@RequestParam(required = false) String storeId, @RequestParam(required = false) String skuCode) {
		EntityType entityType = EntityType.STORE;
		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getAllTicketCategoriesWithoutActive(entityType);
		TicketCategoryNode categoryRoot = ticketCategoryService.getTicketCategoryNodeByLabel(ticketCategoryEntities, TicketCategoryRoot.ORDER_ISSUE.toString());
		if (categoryRoot == null) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, "Any relevant issue ticket category root not found", null));
		}
		List<TicketEntity> tickets = ticketService.getStoreOrderRelatedFilteredTickets(DateUtils.addMinutes(createdFrom, -330),
				DateUtils.addDays(DateUtils.addMinutes(createdTo, -330), 1), categoryRoot.getId(), storeId, skuCode);
		if (tickets == null || tickets.isEmpty()) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, "No tickets found", null));
		}
		List<TicketBean> ticketBeans = getMapper().mapAsList(tickets, TicketBean.class);
		for (TicketBean ticketBean : ticketBeans) {
			for (TicketItemBean itemBean : ticketBean.getItems()) {
				setTicketActionsAndCategory(itemBean, ticketBean.getCategoryRootId(), ticketCategoryEntities);
				setTicketCategoryDesc(itemBean);
			}
		}
		return ResponseEntity.ok(ticketBeans);
	}

	@ApiOperation(value = "fetch lists of tickets for internal", nickname = "fetchTicketsInternalForReturnPickup")
	@GetMapping(path = "/tickets/internal/return-pickup")
	public ResponseEntity<List<ReturnPickupBean>> fetchTicketsInternalForReturnPickup() {
		EntityType entityType = EntityType.STORE;
		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getTicketCategoryByLabels(
				Arrays.asList(TicketCategoryRoot.ORDER_ISSUE.toString()), entityType);
		if (CollectionUtils.isEmpty(ticketCategoryEntities)) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, "Any relevant issue ticket category root not found", null));
		}
		Map<String, Object> filters = new HashMap<>();
		Date currentDateIst = java.sql.Date.valueOf(LocalDate.now(ZoneId.of("Asia/Kolkata")));
		filters.put("categoryRoot", ticketCategoryEntities.stream().map(TicketCategoryEntity::getId).collect(Collectors.toList()));
		filters.put("requesterEntityType", entityType);
		filters.put("fromDate", new FilterCriteria("lastAddedAt", currentDateIst, Operation.GTE));
		Map<String, PageAndSortRequest.SortDirection> sort = new LinkedHashMap<>();
		sort.put("lastAddedAt", PageAndSortRequest.SortDirection.DESC);
		List<TicketEntity> tickets = ticketService.findAllRecords(filters, sort);
		if (CollectionUtils.isEmpty(tickets)) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, String.format("No tickets found"), null));
		}
		List<ReturnPickupBean> ticketBeans = getReturnPickupBeans(currentDateIst, Arrays.asList(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED), tickets);
		return ResponseEntity.ok(ticketBeans);
	}

	private List<ReturnPickupBean> getReturnPickupBeans(Date deliveryDate, List<TicketStatus> statusList, List<TicketEntity> ticketEntities) {
		List<ReturnPickupBean> returnPickupBeans = new ArrayList<>();
		Double issueToDeliveredQtyPickupRatio = Double.valueOf(ParamsUtils.getParam("ISSUE_TO_DELIVERED_QTY_PICKUP_RATIO", "0"));
		for (TicketEntity ticket : ticketEntities) {
			if (ticket.getMetadata().getOrderDetails() != null && ticket.getMetadata().getOrderDetails().getDeliveryDate().equals(deliveryDate)) {
				ReturnPickupBean returnPickupBean = ReturnPickupBean.newInstance();
				returnPickupBean.setStoreId(ticket.getRequesterEntityId());
				returnPickupBean.setOrderId(ticket.getReferenceId());
				returnPickupBean.setItems(getMapper().mapAsList(ticket.getItems().stream()
						.filter(item -> statusList.stream().anyMatch(status -> status.equals(item.getStatus())) && item.getDetails()
								.getOrderDetails() != null && item.getDetails().getOrderDetails().getSkuCode() != null && item.getDetails().getOrderDetails()
								.getProductName() != null && item.getDetails().getOrderDetails().getIssueQty() != null && item.getDetails().getOrderDetails()
								.getDeliveredQty() != null && item.getDetails().getOrderDetails().getIsReturnIssue() && item.getDetails().getOrderDetails()
								.getIssueQty().compareTo(BigDecimal.valueOf(item.getDetails().getOrderDetails().getDeliveredQty())
										.multiply(BigDecimal.valueOf(issueToDeliveredQtyPickupRatio)).doubleValue()) >= 0)
						.map(item -> item.getDetails().getOrderDetails()).collect(Collectors.toList()), ReturnPickupItemDetailsBean.class));
				if (CollectionUtils.isNotEmpty(returnPickupBean.getItems())) {
					returnPickupBeans.add(returnPickupBean);
				}
			}
		}
		return returnPickupBeans;
	}

	@ApiOperation(value = "create tickets for consumer app", nickname = "createTicketsForConsumerApp")
	@PostMapping(path = "/tickets/consumer-app")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(propagation = Propagation.REQUIRED)
	public void createTicketsForConsumerApp(@Valid @RequestBody ConsumerAppCreateTicketRequest createTicketBean) {
		_LOGGER.info(String.format("createTicketsForPartnerApp:: request %s", createTicketBean));
		String requesterEntityId = SessionUtils.getAuthUserId().toString();
		if (requesterEntityId == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "User id not given", null));
		}

		TicketEntity requestTicket = getMapper().mapSrcToDest(createTicketBean, TicketEntity.newInstance());
		if (requestTicket == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No data given to create ticket", null));
		}

		EntityType requesterEntityType = EntityType.USER;
		requestTicket.setRequesterEntityId(requesterEntityId);
		requestTicket.setRequesterEntityType(requesterEntityType);
		requestTicket.setRequesterEntityCategory(TicketConstants.DEFAULT_USER_CATEGORY);

		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getAllTicketCategories(requesterEntityType);
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
		populateNewTicketItems(requestTicketItems, TicketPlatform.CONSUMER_APP);
		populateTicketDetailsAndInvokeCreateOrUpdateActions(requestTicket, requestTicketItems);
		ticketService.saveTicketWithItems(requestTicket, requestTicketItems);
	}

	@ApiOperation(value = "fetch tickets for consumer app", nickname = "fetchTicketsForConsumerApp")
	@GetMapping(path = "/tickets/consumer-app")
	public ResponseEntity<List<TicketBean>> fetchTicketsForConsumerApp() {
		String requesterEntityId = SessionUtils.getAuthUserId().toString();
		if (requesterEntityId == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "User id not given", null));
		}
		EntityType requesterEntityType = EntityType.USER;
		Integer sinceDays = ParamsUtils.getIntegerParam("CONSUMER_APP_TICKET_LIST_DAYS", 10);
		Map<String, Object> filters = new HashMap<>();
		filters.put("requesterEntityId", requesterEntityId);
		filters.put("requesterEntityType", requesterEntityType);
		Date fromDate = DateUtils.addDays(new Date(), -1 * sinceDays);
		filters.put("fromDate", new FilterCriteria("lastAddedAt", fromDate, Operation.GTE));
		Map<String, PageAndSortRequest.SortDirection> sort = new LinkedHashMap<>();
		sort.put("lastAddedAt", PageAndSortRequest.SortDirection.DESC);

		List<TicketEntity> tickets = ticketService.findAllRecords(filters, sort);
		if (tickets == null || tickets.isEmpty()) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, String.format("No tickets found for the last : %d days", sinceDays), null));
		}

		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getAllTicketCategoriesWithoutActive(requesterEntityType);
		List<TicketBean> ticketBeans = getMapper().mapAsList(tickets, TicketBean.class);

		filterTicketOnShowStatus(false, TicketStatus.DRAFT, ticketBeans);
		filterTicketOnShowStatus(false, TicketStatus.CANCELLED, ticketBeans);
		updateOrderDetailsFromClient(ticketBeans);
		for (TicketBean ticketBean : ticketBeans) {
			for (TicketItemBean itemBean : ticketBean.getItems()) {
				setTicketActionsAndCategory(itemBean, ticketBean.getCategoryRootId(), ticketCategoryEntities);
				setTicketCategoryDesc(itemBean);
			}
		}
		return ResponseEntity.ok(ticketBeans);
	}

	@ApiOperation(value = "fetch ticket by reference id", nickname = "fetchTicketByReferenceId")
	@GetMapping(path = "/tickets/reference-id/{id}")
	public ResponseEntity<TicketBean> fetchTicketByReferenceId(@PathVariable String id) {
		TicketEntity ticket = ticketService.findByReferenceIdAndCategoryRootId(id, null).get(0);
		if (ticket == null) {
			throw new ValidationException(
					ErrorBean.withError(Errors.NO_DATA_FOUND, String.format("No data found for ticket with reference id : %s", id), null));
		}
		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getAllTicketCategoriesWithoutActive(ticket.getRequesterEntityType());
		TicketBean ticketBean = getMapper().mapSrcToDest(ticket, TicketBean.newInstance());
		List<TicketBean> ticketBeans = new ArrayList<>();
		ticketBeans.add(ticketBean);
		updateOrderDetailsFromClient(ticketBeans);
		for (TicketItemBean itemBean : ticketBean.getItems()) {
			setTicketActionsAndCategory(itemBean, ticketBean.getCategoryRootId(), ticketCategoryEntities);
			setTicketCategoryDesc(itemBean);
		}
		return ResponseEntity.ok(ticketBean);
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}
}
