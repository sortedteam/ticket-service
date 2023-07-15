package com.sorted.rest.services.ticket.controller;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.ticket.beans.TicketCategoryNode;
import com.sorted.rest.services.ticket.beans.TicketCategoryViewBean;
import com.sorted.rest.services.ticket.entity.TicketCategoryEntity;
import com.sorted.rest.services.ticket.services.TicketCategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Api(tags = "Ticket Categories Services", description = "Manage Ticket Categories related services.")
public class TicketCategoryController implements BaseController {

	AppLogger _LOGGER = LoggingManager.getLogger(TicketCategoryController.class);

	@Autowired
	private TicketCategoryService ticketCategoryService;

	@Autowired
	private BaseMapper<?, ?> mapper;

	@ApiOperation(value = "List all Ticket Categories", nickname = "getVisibleTicketCategories")
	@GetMapping("/tickets/categories")
	public ResponseEntity<List<TicketCategoryNode>> getVisibleTicketCategories(@RequestParam(required = false) String label,
			@RequestParam(defaultValue = "true") Boolean showOnlyVisible) {
		List<TicketCategoryNode> ticketCategoryNodes = new ArrayList<>();
		List<TicketCategoryEntity> ticketCategoryEntities;
		if (showOnlyVisible) {
			ticketCategoryEntities = ticketCategoryService.getVisibleTicketCategories();
		} else {
			ticketCategoryEntities = ticketCategoryService.findAllRecords();
		}

		if (label == null) {
			ticketCategoryNodes = ticketCategoryService.getAllTicketCategoryNodes(ticketCategoryEntities);
		} else {
			TicketCategoryNode ticketCategoryNode = ticketCategoryService.getTicketCategoryNodeByLabel(ticketCategoryEntities, label);
			if (ticketCategoryNode != null) {
				ticketCategoryNodes.add(ticketCategoryNode);
			}
		}
		return ResponseEntity.ok(ticketCategoryNodes);
	}

	@ApiOperation(value = "get leaf filters", nickname = "getAllLeafFilters")
	@GetMapping(path = "/tickets/categories/leaf-filter")
	public ResponseEntity<List<TicketCategoryViewBean>> getAllLeafFilters(@RequestParam Integer filterGroup) {
		Map<String, Object> filters = new HashMap<>();
		filters.put("filterGroup", filterGroup);
		return ResponseEntity.ok(getMapper().mapAsList(ticketCategoryService.findAllRecords(filters), TicketCategoryViewBean.class));
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}
}
