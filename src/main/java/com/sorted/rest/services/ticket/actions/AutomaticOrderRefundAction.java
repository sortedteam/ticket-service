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

	static AppLogger _LOGGER = LoggingManager.getLogger(AutomaticOrderRefundAction.class);

	@Autowired
	private TicketHistoryService ticketHistoryService;

	@Override
	public Boolean isApplicable(TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		if (ticket.getResolutionDetails().getOrderDetails() != null && ticket.getResolutionDetails().getOrderDetails()
				.getIssueQty() != null && ticket.getResolutionDetails().getOrderDetails().getRefundableQty() != null) {
			return ticket.getResolutionDetails().getOrderDetails().getRefundableQty()
					.compareTo(ticket.getResolutionDetails().getOrderDetails().getIssueQty()) != -1;
		}
		return false;
	}

	@Override
	public Boolean apply(TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		Boolean terminate = false;
		try {
			ticket.setAssignedTeam(TicketResolutionTeam.CUSTOMERCARE.toString());
			ticket.setAssignedAt(new Date());
			ticket.setRemarks(TicketConstants.AUTOMATIC_ORDER_REFUND_REMARKS);
			actionDetailsBean.setRemarks(TicketConstants.AUTOMATIC_ORDER_REFUND_REMARKS);
			ticketHistoryService.addTicketHistory(ticket.getId(), action, actionDetailsBean);
			terminate = true;
		} catch (Exception e) {
			_LOGGER.error(String.format("Error in applying for action TestTicketActions for ticket :%s ", ticket), e);
		}
		return terminate;
	}
}