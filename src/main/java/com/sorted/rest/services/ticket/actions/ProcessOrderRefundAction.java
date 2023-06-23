package com.sorted.rest.services.ticket.actions;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketStatus;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ProcessOrderRefundAction implements TicketActionsInterface {

	static AppLogger _LOGGER = LoggingManager.getLogger(ProcessOrderRefundAction.class);

	@Autowired
	private TicketHistoryService ticketHistoryService;

	private String remarks;

	public void setRemarks(String ticketRemarks) {
		remarks = ticketRemarks;
	}

	@Override
	public Boolean isApplicable(TicketItemEntity item, Long ticketId, String action, TicketActionDetailsBean actionDetailsBean) {
		if (item.getResolutionDetails().getOrderDetails() != null && item.getResolutionDetails().getOrderDetails()
				.getIssueQty() != null && item.getResolutionDetails().getOrderDetails().getRefundableQty() != null) {
			return item.getResolutionDetails().getOrderDetails().getRefundableQty()
					.compareTo(item.getResolutionDetails().getOrderDetails().getIssueQty()) != -1;
		}
		return false;
	}

	@Override
	public Boolean apply(TicketItemEntity item, Long ticketId, String action, TicketActionDetailsBean actionDetailsBean) {
		item.setAssignedTeam(TicketConstants.CLOSED_TICKET_ASSIGNED_TEAM);
		item.setAssignedAt(new Date());
		item.setRemarks(remarks);
		item.setStatus(TicketStatus.CLOSED.toString());
		actionDetailsBean.setRemarks(remarks);
		ticketHistoryService.addTicketHistory(ticketId, item.getId(), action, actionDetailsBean);
		return true;
	}
}