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
public class AutomaticOrderRefundAction implements TicketActionsInterface {

	static AppLogger _LOGGER = LoggingManager.getLogger(EscalateToCustomercareAction.class);

	@Autowired
	private TicketHistoryService ticketHistoryService;

	@Override
	public Boolean isApplicable(TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		try {
			return false;
		} catch (Exception e) {
			_LOGGER.error(String.format("Error in checking application for action TestTicketActions for ticket :%s ", ticket), e);
		}
		return false;
	}

	@Override
	public Boolean apply(TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		Boolean terminate = false;
		try {
			ticket.setAssignedTeam(TicketResolutionTeam.CUSTOMERCARE.toString());
			ticket.setAssignedAt(new Date());
			actionDetailsBean.setRemarks(TicketConstants.AUTOMATIC_ORDER_REFUND_REMARKS);
			ticketHistoryService.addTicketHistory(ticket.getId(), action, actionDetailsBean);
			terminate = true;
		} catch (Exception e) {
			_LOGGER.error(String.format("Error in applying for action TestTicketActions for ticket :%s ", ticket), e);
		}
		return terminate;
	}
}