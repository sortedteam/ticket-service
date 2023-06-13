package com.sorted.rest.services.ticket.actions;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketResolutionTeam;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class EscalateToTeamAction implements TicketActionsInterface {

	static AppLogger _LOGGER = LoggingManager.getLogger(EscalateToTeamAction.class);

	@Autowired
	private TicketHistoryService ticketHistoryService;

	private String team;

	private String remarks;

	public void setTeamAndRemarks(TicketResolutionTeam ticketResolutionTeam, String ticketRemarks) {
		team = ticketResolutionTeam.toString();
		remarks = ticketRemarks;
	}

	@Override
	public Boolean isApplicable(TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		return !ticket.getAssignedTeam().equals(team);
	}

	@Override
	public Boolean apply(TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		Boolean terminate = true;
		ticket.setAssignedTeam(team);
		ticket.setAssignedAt(new Date());
		ticket.setRemarks(remarks);
		actionDetailsBean.setRemarks(remarks);
		ticketHistoryService.addTicketHistory(ticket.getId(), action, actionDetailsBean);
		return terminate;
	}
}
