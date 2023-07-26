package com.sorted.rest.services.ticket.actions;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.services.ticket.beans.OrderItemDetailsBean;
import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class AutomaticFullOrderRefundAction implements TicketActionsInterface {

	static AppLogger _LOGGER = LoggingManager.getLogger(AutomaticFullOrderRefundAction.class);

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
		if (ticket.getMetadata().getOrderDetails() != null && ticket.getMetadata().getOrderDetails().getOrderId() != null && ticket.getMetadata()
				.getOrderDetails().getFinalOrderBillAmount() != null) {
			OrderItemDetailsBean orderItemDetailsBean = OrderItemDetailsBean.newInstance();
			orderItemDetailsBean.setProductName(TicketConstants.FULL_ORDER_REFUND_PRODUCT_NAME);
			orderItemDetailsBean.setOrderId(ticket.getMetadata().getOrderDetails().getOrderId());
			orderItemDetailsBean.setRefundableAmount(ticket.getMetadata().getOrderDetails().getFinalOrderBillAmount());
			orderItemDetailsBean.setIsReturnIssue(true);
			item.getDetails().setOrderDetails(orderItemDetailsBean);

			ticket.getMetadata().getOrderDetails().setTotalRefundableAmount(BigDecimal.valueOf(
					ticket.getMetadata().getOrderDetails().getTotalRefundableAmount() != null ?
							ticket.getMetadata().getOrderDetails().getTotalRefundableAmount() :
							0d).add(BigDecimal.valueOf(item.getDetails().getOrderDetails().getRefundableAmount())).doubleValue());
			return true;
		}
		return false;
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