package com.sorted.rest.services.ticket.actions;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketResolutionTeam;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class EscalateToWarehouseAction implements TicketActionsInterface {

	// Escalate to warehouse
	static AppLogger _LOGGER = LoggingManager.getLogger(EscalateToWarehouseAction.class);

	@Autowired
	private TicketHistoryService ticketHistoryService;

	@Override
	public Boolean isApplicable(TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		return true;
	}

	@Override
	public Boolean apply(TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		Boolean terminate = true;
		ticket.setAssignedTeam(TicketResolutionTeam.WAREHOUSE.toString());
		ticket.setAssignedAt(new Date());
		actionDetailsBean.setRemarks(TicketConstants.ESCALATE_TO_WAREHOUSE_REMARKS);
		ticketHistoryService.addTicketHistory(ticket.getId(), action, actionDetailsBean);
		return terminate;
	}
}
