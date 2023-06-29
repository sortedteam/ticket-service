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
	public Boolean isApplicable(TicketItemEntity item, Long ticketId, String action, TicketActionDetailsBean actionDetailsBean) {
		if (item.getDetails().getOrderDetails() != null) {

			item.getDetails().getOrderDetails().setIsReturnIssue(true);
			if (item.getDetails().getOrderDetails().getProrataAmount() != null && item.getDetails().getOrderDetails()
					.getDeliveredQty() != null && item.getDetails().getOrderDetails().getIssueQty() != null) {

				item.getDetails().getOrderDetails().setRefundableAmount(BigDecimal.valueOf(item.getDetails().getOrderDetails().getProrataAmount())
						.divide(BigDecimal.valueOf(item.getDetails().getOrderDetails().getDeliveredQty()))
						.multiply(BigDecimal.valueOf(item.getDetails().getOrderDetails().getIssueQty())).setScale(2, RoundingMode.HALF_UP).doubleValue());
			}

			if (item.getDetails().getOrderDetails().getIssueQty() != null && item.getDetails().getOrderDetails().getRefundableQty() != null) {
				return item.getDetails().getOrderDetails().getRefundableQty().compareTo(item.getDetails().getOrderDetails().getIssueQty()) != -1;
			}
		}
		return false;
	}

	@Override
	public Boolean apply(TicketItemEntity item, Long ticketId, String action, TicketActionDetailsBean actionDetailsBean) {
		item.getDetails().getOrderDetails().setIsAutoRefundEligible(true);
		item.setAssignedTeam(team);
		item.setAssignedAt(new Date());
		item.setRemarks(remarks);
		actionDetailsBean.setRemarks(remarks);
		actionDetailsBean.setAttachments(attachments);
		ticketHistoryService.addTicketHistory(ticketId, item.getId(), action, actionDetailsBean);
		return true;
	}
}