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
import com.sorted.rest.services.ticket.repository.TicketRepository;
import com.sorted.rest.services.ticket.services.TicketCategoryService;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import com.sorted.rest.services.ticket.services.TicketService;
import com.sorted.rest.services.ticket.utils.TicketActionUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

	@ApiOperation(value = "create a ticket", nickname = "createTicket")
	@PostMapping(path = "/tickets/partner-app")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(propagation = Propagation.REQUIRED)
	public List<TicketBean> createTickets(@RequestBody List<PartnerAppCreateTicketRequest> createTicketBeans) {
		List<TicketEntity> tickets = getMapper().mapAsList(createTicketBeans, TicketEntity.class);
		List<TicketCategoryEntity> ticketCategoryEntities = ticketCategoryService.getVisibleTicketCategories();
		Map<Integer, TicketCategoryEntity> categoryMap = new HashMap<>();
		for (TicketCategoryEntity category : ticketCategoryEntities) {
			categoryMap.put(category.getId(), category);
		}
		validateCommonDetailsForPartnerAppRequest(tickets);
		validateTicketCategories(tickets, categoryMap);
		for (TicketEntity ticket : tickets) {
			if (ticket.getPriority() == null) {
				ticket.setPriority(ticket.getCategoryLeaf().getPriority());
			}
			if (ticket.getCategoryLeaf().getIsTerminal() != 1) {
				ticket.setStatus(TicketStatus.DRAFT.toString());
			} else {
				ticket.setStatus(TicketStatus.IN_PROGRESS.toString());
			}
			ticket.setRemarks("New Ticket Created");
			ticket.setAssignedTeam(TicketResolutionTeam.CUSTOMERCARE.toString());
			ticket.setAssignedAt(new Date());
		}
		tickets = ticketService.saveAll(tickets);
		for (TicketEntity ticket : tickets) {
			if (ticket.getStatus().equals(TicketStatus.DRAFT.toString())) {
				ticketActionUtils.invokeTicketDraftAction(ticket);
			} else if (ticket.getStatus().equals(TicketStatus.IN_PROGRESS.toString())) {
				ticketActionUtils.invokeTicketRaiseAction(ticket, ticket.getCategoryLeaf().getOnCreateActions());
			}
		}
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

		for (TicketEntity ticket : tickets) {
			if (!ticket.getReferenceId().equals(referenceId)) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Ticket referenceId must be common for the group of tickets", null));
			}
		}

		List<String> storeCategoryForTicketParam = Arrays.stream(paramService.getParam("STORE_CATEGORY_FOR_TICKET", "Green|").split("\\|"))
				.collect(Collectors.toList());

		String requesterEntityCategory = clientService.getFilteredOrDefaultAudience(requesterEntityId, requesterEntityType,
				Arrays.stream(storeCategoryForTicketParam.get(1).split(",")).filter(s -> !StringUtils.isEmpty(s) && !StringUtils.isEmpty(s.trim()))
						.map(String::trim).distinct().collect(Collectors.toList()), storeCategoryForTicketParam.get(0));

		tickets.forEach(ticket -> {
			ticket.setRequesterEntityId(requesterEntityId);
			ticket.setRequesterEntityType(requesterEntityType);
			ticket.setRequesterEntityCategory(requesterEntityCategory);
		});

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
			List<String> storeCategoryForTicketParam = Arrays.stream(paramService.getParam("STORE_CATEGORY_FOR_TICKET", "Green|").split("\\|"))
					.collect(Collectors.toList());
			String requesterEntityCategory = clientService.getFilteredOrDefaultAudience(requesterEntityId, requesterEntityType,
					Arrays.stream(storeCategoryForTicketParam.get(1).split(",")).filter(s -> !StringUtils.isEmpty(s) && !StringUtils.isEmpty(s.trim()))
							.map(String::trim).distinct().collect(Collectors.toList()), storeCategoryForTicketParam.get(0));
			tickets.forEach(t -> t.setRequesterEntityCategory(requesterEntityCategory));
		}

	}

	private void validateTicketCategories(List<TicketEntity> tickets, Map<Integer, TicketCategoryEntity> categoryMap) {
		for (TicketEntity ticket : tickets) {
			Boolean rootFound = categoryMap.containsKey(ticket.getCategoryRootId());
			Boolean leafFound = categoryMap.containsKey(ticket.getCategoryLeafId());
			if (!rootFound) {
				throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND,
						String.format("Ticket category with id : %d not found, please retry", ticket.getCategoryRootId()), "categoryNotFound"));
			}
			if (!leafFound) {
				throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND,
						String.format("Ticket category with id : %d not found, please retry", ticket.getCategoryLeafId()), "categoryNotFound"));
			} else {
				ticket.setCategoryLeaf(categoryMap.get(ticket.getCategoryLeafId()));
			}
		}
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}
}
