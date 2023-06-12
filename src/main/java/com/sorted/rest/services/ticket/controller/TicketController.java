package com.sorted.rest.services.ticket.controller;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.params.service.ParamService;
import com.sorted.rest.services.ticket.beans.PartnerAppCreateTicketRequest;
import com.sorted.rest.services.ticket.beans.TicketBean;
import com.sorted.rest.services.ticket.clients.ClientService;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import com.sorted.rest.services.ticket.constants.TicketConstants.EntityType;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketResolutionTeam;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketStatus;
import com.sorted.rest.services.ticket.entity.TicketCategoryEntity;
import com.sorted.rest.services.ticket.entity.TicketEntity;
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

import java.util.*;
import java.util.stream.Collectors;

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

	@ApiOperation(value = "create a ticket", nickname = "createTicket")
	@PostMapping(path = "/tickets/partner-app")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(propagation = Propagation.REQUIRED)
	public List<TicketBean> createTicketsForPartnerApp(@RequestBody List<PartnerAppCreateTicketRequest> createTicketBeans) {
		List<TicketEntity> tickets = getMapper().mapAsList(createTicketBeans, TicketEntity.class);
		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getVisibleTicketCategories();
		Map<Integer, TicketCategoryEntity> categoryMap = new HashMap<>();
		for (TicketCategoryEntity category : ticketCategoryEntities) {
			categoryMap.put(category.getId(), category);
		}
		validateCommonDetailsForPartnerAppRequest(tickets);
		validateTicketCategories(tickets, categoryMap);
		for (TicketEntity ticket : tickets) {
			if (ticket.getCategoryLeaf().getIsTerminal() != 1) {
				ticket.setStatus(TicketStatus.DRAFT.toString());
			} else {
				ticket.setStatus(TicketStatus.IN_PROGRESS.toString());
			}
			ticket.setPriority(ticket.getCategoryLeaf().getPriority());
			ticket.setRemarks(TicketConstants.NEW_TICKET_CREATED_REMARKS);
			ticket.setAssignedTeam(TicketResolutionTeam.CUSTOMERCARE.toString());
			ticket.setAssignedAt(new Date());
		}
		tickets = ticketService.saveAll(tickets);
		populateTicketDetailsAndInvokeActions(tickets);
		tickets = ticketService.saveAll(tickets);
		List<TicketBean> ticketBeans = getMapper().mapAsList(tickets, TicketBean.class);
		for (TicketBean ticketBean : ticketBeans) {
			ticketBean.setCategory(ticketCategoryService.getRootToLeafPathUsingCategoryList(ticketCategoryEntities, ticketBean.getCategoryRootId(),
					ticketBean.getCategoryLeafId()));
			ticketBean.setAssignedAt(DateUtils.convertDateUtcToIst(ticketBean.getAssignedAt()));
			ticketBean.setCreatedAt(DateUtils.convertDateUtcToIst(ticketBean.getCreatedAt()));
			ticketBean.setModifiedAt(DateUtils.convertDateUtcToIst(ticketBean.getModifiedAt()));
		}
		return ticketBeans;
	}

	private void validateCommonDetailsForPartnerAppRequest(List<TicketEntity> tickets) {
		if (tickets == null || tickets.isEmpty()) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No data given to create ticket", null));
		}

		String referenceId = tickets.get(0).getReferenceId();
		String requesterEntityId = SessionUtils.getStoreId();
		String requesterEntityType = EntityType.STORE.toString();
		Integer categoryRootId = tickets.get(0).getCategoryRootId();

		for (TicketEntity ticket : tickets) {
			if (referenceId != null && !ticket.getReferenceId().equals(referenceId)) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Ticket referenceId must be common for the group of tickets", null));
			}
			if (ticket.getCategoryRootId() != categoryRootId) {
				throw new ValidationException(
						ErrorBean.withError(Errors.INVALID_REQUEST, "Ticket category root id must be common for the group of tickets", null));
			}
		}

		String requesterEntityCategory = getStoreCategoryForTicket(requesterEntityId, requesterEntityType);

		tickets.forEach(ticket -> {
			ticket.setRequesterEntityId(requesterEntityId);
			ticket.setRequesterEntityType(requesterEntityType);
			ticket.setRequesterEntityCategory(requesterEntityCategory);
		});

	}

	private void populateTicketDetailsAndInvokeActions(List<TicketEntity> tickets) {
		ticketRequestUtils.populateTicketRequestAsPerCategoryRoot(tickets);
		ticketActionUtils.populateTicketDetailsAsPerCategoryRoot(tickets);
		for (TicketEntity ticket : tickets) {
			ticketActionUtils.invokeTicketCreateAction(ticket);
			if (ticket.getStatus().equals(TicketStatus.IN_PROGRESS.toString())) {
				ticketActionUtils.invokeTicketRaiseAction(ticket);
			}
		}
		ticketRequestUtils.clearTicketRequest();
	}

	private void validateCommonDetailsForBackofficeRequest(List<TicketEntity> tickets) {
		if (tickets == null || tickets.isEmpty()) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "No data given to create ticket", null));
		}

		Set<String> userRoles = SessionUtils.getAuthUserRoles();
		String referenceId = tickets.get(0).getReferenceId();
		String requesterEntityId = tickets.get(0).getRequesterEntityId();
		String requesterEntityType = tickets.get(0).getRequesterEntityType();

		for (TicketEntity ticket : tickets) {
			if (!ticket.getReferenceId().equals(referenceId)) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Ticket referenceId must be common for the group of tickets", null));
			}
			if (!ticket.getRequesterEntityId().equals(requesterEntityId)) {
				throw new ValidationException(
						ErrorBean.withError(Errors.INVALID_REQUEST, "Ticket requester entity id must be common for the group of tickets", null));
			}
			if (!ticket.getRequesterEntityType().equals(requesterEntityType)) {
				throw new ValidationException(
						ErrorBean.withError(Errors.INVALID_REQUEST, "Ticket requester entity type must be common for the group of tickets", null));
			}
			if (!userRoles.contains(TicketConstants.CUSTOMER_CARE_ROLE)) {
				ticket.setPriority(null);
				ticket.setAssignedTeam(null);
				ticket.setAssignedAt(null);
			}
		}

		if (requesterEntityType.equals(EntityType.STORE.toString())) {
			String requesterEntityCategory = getStoreCategoryForTicket(requesterEntityId, requesterEntityType);
			tickets.forEach(t -> t.setRequesterEntityCategory(requesterEntityCategory));
		}

	}

	private String getStoreCategoryForTicket(String storeId, String entityType) {
		List<String> storeCategoryForTicketParam = Arrays.stream(paramService.getParam("STORE_CATEGORY_FOR_TICKET", "Green|").split("\\|"))
				.collect(Collectors.toList());
		String requesterEntityCategory = clientService.getFilteredOrDefaultAudience(storeId, entityType,
				Arrays.stream(storeCategoryForTicketParam.get(1).split(",")).filter(s -> !StringUtils.isEmpty(s) && !StringUtils.isEmpty(s.trim()))
						.map(String::trim).distinct().collect(Collectors.toList()), storeCategoryForTicketParam.get(0));
		return requesterEntityCategory;
	}

	private void validateTicketCategories(List<TicketEntity> tickets, Map<Integer, TicketCategoryEntity> categoryMap) {
		for (TicketEntity ticket : tickets) {
			Boolean rootFound = categoryMap.containsKey(ticket.getCategoryRootId());
			Boolean leafFound = categoryMap.containsKey(ticket.getCategoryLeafId());
			if (!rootFound) {
				throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND,
						String.format("Ticket category with id : %d not found, please retry", ticket.getCategoryRootId()), "categoryNotFound"));
			} else if (!leafFound) {
				throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND,
						String.format("Ticket category with id : %d not found, please retry", ticket.getCategoryLeafId()), "categoryNotFound"));
			} else {
				ticket.setCategoryRoot(categoryMap.get(ticket.getCategoryRootId()));
				ticket.setCategoryLeaf(categoryMap.get(ticket.getCategoryLeafId()));
			}
		}
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
