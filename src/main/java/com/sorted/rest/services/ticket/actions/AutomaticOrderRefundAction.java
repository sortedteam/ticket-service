package com.sorted.rest.services.ticket.actions;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Component
public class AutomaticOrderRefundAction implements TicketActionsInterface {

	static AppLogger _LOGGER = LoggingManager.getLogger(AutomaticOrderRefundAction.class);

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
	public Boolean isApplicable(TicketItemEntity item, TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		if (item.getDetails().getOrderDetails() != null && ticket.getMetadata().getOrderDetails() != null) {
			item.getDetails().getOrderDetails().setIsReturnIssue(true);
			if (item.getDetails().getOrderDetails().getProrataAmount() != null && item.getDetails().getOrderDetails().getIssueQty() != null && item.getDetails()
					.getOrderDetails().getDeliveredQty() != null && item.getDetails().getOrderDetails().getDeliveredQty().compareTo(0d) == 1) {
				item.getDetails().getOrderDetails().setRefundableAmount(BigDecimal.valueOf(item.getDetails().getOrderDetails().getProrataAmount())
						.multiply(BigDecimal.valueOf(item.getDetails().getOrderDetails().getIssueQty()))
						.divide(BigDecimal.valueOf(item.getDetails().getOrderDetails().getDeliveredQty()), 2, RoundingMode.HALF_UP).doubleValue());
			}
			if (item.getDetails().getOrderDetails().getIssueQty() != null && item.getDetails().getOrderDetails().getRefundableQty() != null) {
				ticket.getMetadata().getOrderDetails().setTotalRefundableAmount(BigDecimal.valueOf(
						ticket.getMetadata().getOrderDetails().getTotalRefundableAmount() != null ?
								ticket.getMetadata().getOrderDetails().getTotalRefundableAmount() :
								0d).add(BigDecimal.valueOf(item.getDetails().getOrderDetails().getRefundableAmount())).doubleValue());
				return item.getDetails().getOrderDetails().getRefundableQty().compareTo(item.getDetails().getOrderDetails().getIssueQty()) != -1;
			}
		}
		return false;
	}

	@Override
	public Boolean apply(TicketItemEntity item, TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		item.getDetails().getOrderDetails().setIsAutoRefundEligible(true);
		item.setAssignedTeam(team);
		item.setAssignedAt(new Date());
		item.setRemarks(remarks);
		actionDetailsBean.setRemarks(remarks);
		actionDetailsBean.setAttachments(attachments);
		ticketHistoryService.addTicketHistory(ticket.getId(), item.getId(), action, actionDetailsBean);
		return true;
	}
}