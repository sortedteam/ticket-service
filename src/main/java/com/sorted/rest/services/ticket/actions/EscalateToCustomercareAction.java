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
public class EscalateToCustomercareAction implements TicketActionsInterface {

	// Escalate to customer care
	static AppLogger _LOGGER = LoggingManager.getLogger(EscalateToCustomercareAction.class);

	@Autowired
	private TicketHistoryService ticketHistoryService;

	@Override
	public Boolean isApplicable(TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		return true;
	}

	@Override
	public Boolean apply(TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		Boolean terminate = true;
		ticket.setAssignedTeam(TicketResolutionTeam.CUSTOMERCARE.toString());
		ticket.setAssignedAt(new Date());
		ticket.setRemarks(TicketConstants.ESCALATE_TO_CUSTOMERCARE_REMARKS);
		actionDetailsBean.setRemarks(TicketConstants.ESCALATE_TO_CUSTOMERCARE_REMARKS);
		ticketHistoryService.addTicketHistory(ticket.getId(), action, actionDetailsBean);
		return terminate;
	}
}
