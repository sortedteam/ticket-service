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
import java.util.List;

@Component
public class AutomaticConsumerOrderRefundAction implements TicketActionsInterface {

	static AppLogger _LOGGER = LoggingManager.getLogger(AutomaticConsumerOrderRefundAction.class);

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
		if (item.getDetails().getConsumerOrderDetails() == null || item.getDetails().getConsumerOrderDetails().getIssueQty() == null || item.getDetails()
				.getConsumerOrderDetails().getSkuCode() == null) {
			_LOGGER.error("Consumer Order related ticket missing issue quantity or sku code");
			return false;
		}

		if (ticket.getMetadata().getConsumerOrderDetails() != null) {
			item.getDetails().getConsumerOrderDetails().setIsRefundIssue(true);
			item.getDetails().getConsumerOrderDetails()
					.setIssueQty(BigDecimal.valueOf(item.getDetails().getConsumerOrderDetails().getIssueQty()).setScale(3, RoundingMode.FLOOR).doubleValue());
			if (item.getDetails().getConsumerOrderDetails().getProrataAmount() != null && item.getDetails().getConsumerOrderDetails()
					.getDeliveredQty() != null && item.getDetails().getConsumerOrderDetails().getDeliveredQty().compareTo(0d) == 1) {
				item.getDetails().getConsumerOrderDetails().setRefundableAmount(
						BigDecimal.valueOf(item.getDetails().getConsumerOrderDetails().getProrataAmount())
								.multiply(BigDecimal.valueOf(item.getDetails().getConsumerOrderDetails().getIssueQty()))
								.divide(BigDecimal.valueOf(item.getDetails().getConsumerOrderDetails().getDeliveredQty()), 0, RoundingMode.FLOOR)
								.doubleValue());

				ticket.getMetadata().getConsumerOrderDetails().setTotalRefundableAmount(BigDecimal.valueOf(
						ticket.getMetadata().getConsumerOrderDetails().getTotalRefundableAmount() != null ?
								ticket.getMetadata().getConsumerOrderDetails().getTotalRefundableAmount() :
								0d).add(BigDecimal.valueOf(item.getDetails().getConsumerOrderDetails().getRefundableAmount())).doubleValue());
			}
			//			if (item.getDetails().getConsumerOrderDetails().getRefundableQty() != null) {
			//				return item.getDetails().getConsumerOrderDetails().getRefundableQty()
			//						.compareTo(item.getDetails().getConsumerOrderDetails().getIssueQty()) != -1;
			//			}
		}
		return true;
	}

	@Override
	public Boolean apply(TicketItemEntity item, TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		item.setRemarks(remarks);
		actionDetailsBean.setRemarks(remarks);
		actionDetailsBean.setAttachments(attachments);
		ticketHistoryService.addTicketHistory(ticket.getId(), item.getId(), action, actionDetailsBean);
		return true;
	}
}