package com.sorted.rest.services.ticket.actions;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class CloseTicketAction implements TicketActionsInterface {

	static AppLogger _LOGGER = LoggingManager.getLogger(CloseTicketAction.class);

	@Autowired
	private TicketHistoryService ticketHistoryService;

	private String team;

	private String remarks;

	public void setTeamAndRemarks(String ticketResolutionTeam, String ticketRemarks) {
		team = ticketResolutionTeam;
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
		item.setAssignedTeam(team);
		item.setAssignedAt(new Date());
		item.setRemarks(remarks);
		actionDetailsBean.setRemarks(remarks);
		ticketHistoryService.addTicketHistory(ticketId, item.getId(), action, actionDetailsBean);
		return true;
	}
}