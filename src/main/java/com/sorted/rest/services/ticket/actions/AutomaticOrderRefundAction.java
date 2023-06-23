package com.sorted.rest.services.ticket.actions;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

@Component
public class AutomaticOrderRefundAction implements TicketActionsInterface {

	static AppLogger _LOGGER = LoggingManager.getLogger(AutomaticOrderRefundAction.class);

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
		if (item.getResolutionDetails().getOrderDetails() != null) {

			item.getResolutionDetails().getOrderDetails().setIsRefundEligible(true);
			if (item.getResolutionDetails().getOrderDetails().getProrataAmount() != null && item.getResolutionDetails().getOrderDetails()
					.getDeliveredQty() != null && item.getResolutionDetails().getOrderDetails().getIssueQty() != null) {
				item.getResolutionDetails().getOrderDetails().setRefundAmount(
						BigDecimal.valueOf(item.getResolutionDetails().getOrderDetails().getProrataAmount())
								.divide(BigDecimal.valueOf(item.getResolutionDetails().getOrderDetails().getDeliveredQty()))
								.multiply(BigDecimal.valueOf(item.getResolutionDetails().getOrderDetails().getIssueQty())).setScale(2, RoundingMode.HALF_UP)
								.doubleValue());
			}

			if (item.getResolutionDetails().getOrderDetails().getIssueQty() != null && item.getResolutionDetails().getOrderDetails()
					.getRefundableQty() != null) {
				return item.getResolutionDetails().getOrderDetails().getRefundableQty()
						.compareTo(item.getResolutionDetails().getOrderDetails().getIssueQty()) != -1;
			}
		}
		return false;
	}

	@Override
	public Boolean apply(TicketItemEntity item, Long ticketId, String action, TicketActionDetailsBean actionDetailsBean) {
		item.getResolutionDetails().getOrderDetails().setIsAutoRefundEligible(true);
		item.setAssignedTeam(team);
		item.setAssignedAt(new Date());
		item.setRemarks(remarks);
		actionDetailsBean.setRemarks(remarks);
		ticketHistoryService.addTicketHistory(ticketId, item.getId(), action, actionDetailsBean);
		return true;
	}
}