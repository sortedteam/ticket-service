package com.sorted.rest.services.ticket.controller;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.params.service.ParamService;
import com.sorted.rest.services.ticket.beans.CreateTicketBean;
import com.sorted.rest.services.ticket.beans.TicketBean;
import com.sorted.rest.services.ticket.clients.ClientService;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.services.TicketCategoryService;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import com.sorted.rest.services.ticket.services.TicketService;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Api(tags = "Ticket Services", description = "Manage Ticket related services.")
public class TicketController implements BaseController {

	AppLogger _LOGGER = LoggingManager.getLogger(TicketController.class);

	@Autowired
	private TicketService ticketService;

	@Autowired
	private TicketHistoryService ticketHistoryService;

	@Autowired
	private TicketCategoryService ticketCategoryService;

	@Autowired
	private BaseMapper<?, ?> mapper;

	@Autowired
	private ClientService clientService;

	@Autowired
	private ParamService paramService;

	@ApiOperation(value = "create a ticket", nickname = "createTicket")
	@PostMapping(path = "/tickets")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(propagation = Propagation.REQUIRED)
	public TicketBean createTickets(@RequestBody CreateTicketBean createTicketBean) {
		TicketEntity ticket = getMapper().mapSrcToDest(createTicketBean, TicketEntity.newInstance());
		List<String> storeCategoryForTicketParam = Arrays.stream(paramService.getParam("STORE_CATEGORY_FOR_TICKET", "Green|").split("\\|"))
				.collect(Collectors.toList());
		String requesterEntityCategory = clientService.getFilteredOrDefaultAudience(ticket.getRequesterEntityId(), ticket.getRequesterEntityType(),
				Arrays.stream(storeCategoryForTicketParam.get(1).split(",")).filter(s -> !StringUtils.isEmpty(s) && !StringUtils.isEmpty(s.trim()))
						.map(String::trim).distinct().collect(Collectors.toList()), storeCategoryForTicketParam.get(0));
		ticket.setRequesterEntityCategory(requesterEntityCategory);
		return null;
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}
}
