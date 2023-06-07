package com.sorted.rest.services.ticket.controller;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.ticket.beans.TicketCategoryNode;
import com.sorted.rest.services.ticket.services.TicketCategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mohit on 19.6.20.
 */
@RestController
@Api(tags = "Order Services", description = "Manage Order related services.")
public class TicketCategoryController implements BaseController {

	AppLogger _LOGGER = LoggingManager.getLogger(TicketCategoryController.class);

	@Autowired
	private TicketCategoryService ticketCategoryService;

	@Autowired
	private BaseMapper<?, ?> mapper;

	@ApiOperation(value = "List all Ticket Categories", nickname = "getVisibleTicketCategories")
	@GetMapping("/tickets/categories")
	public ResponseEntity<List<TicketCategoryNode>> getVisibleTicketCategories(@RequestParam(required = false) Integer id) {
		List<TicketCategoryNode> ticketCategoryNodes = new ArrayList<>();
		if (id == null) {
			ticketCategoryNodes = ticketCategoryService.getVisibleTicketCategoryNodes();
		} else {
			ticketCategoryNodes.add(ticketCategoryService.getTicketCategoryNodeById(id));
		}
		//		String response = "[]";
		//		try {
		//			response = getMapper().getJacksonMapper().writeValueAsString(ticketCategoryNodes);
		//		} catch (JsonProcessingException e) {
		//			_LOGGER.error("Error while fetching ticket categories", e);
		//		}
		return ResponseEntity.ok(ticketCategoryNodes);
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}
}
