package com.sorted.rest.services.ticket.actions;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class EscalateToTeamAction implements TicketActionsInterface {

	static AppLogger _LOGGER = LoggingManager.getLogger(EscalateToTeamAction.class);

	@Autowired
	private TicketHistoryService ticketHistoryService;

	private String team;

	private String remarks;

	private List<String> attachments;

	public void setAttachments(List<String> ticketAttachments) {
		attachments = ticketAttachments;
	}

	public void setTeamAndRemarks(String ticketResolutionTeam, String ticketRemarks) {
		team = ticketResolutionTeam;
		remarks = ticketRemarks;
	}

	@Override
	public Boolean isApplicable(TicketItemEntity item, Long ticketId, String action, TicketActionDetailsBean actionDetailsBean) {
		return !item.getAssignedTeam().equals(team);
	}

	@Override
	public Boolean apply(TicketItemEntity item, Long ticketId, String action, TicketActionDetailsBean actionDetailsBean) {
		Boolean terminate = true;
		item.setAssignedTeam(team);
		item.setAssignedAt(new Date());
		item.setRemarks(remarks);
		actionDetailsBean.setRemarks(remarks);
		actionDetailsBean.setAttachments(attachments);
		ticketHistoryService.addTicketHistory(ticketId, item.getId(), action, actionDetailsBean);
		return terminate;
	}
}
