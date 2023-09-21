package com.sorted.rest.services.ticket.actions;

import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.services.ticket.beans.*;
import com.sorted.rest.services.ticket.clients.TicketClientService;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketCreateActions;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketStatus;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Component
public class AutomaticOrderCancelAction implements TicketActionsInterface {

	static AppLogger _LOGGER = LoggingManager.getLogger(AutomaticOrderCancelAction.class);

	@Autowired
	private TicketHistoryService ticketHistoryService;

	private String team;

	private String remarks;

	private List<String> attachments;

	@Autowired
	private TicketClientService ticketClientService;

	public void setAttachments(List<String> ticketAttachments) {
		attachments = ticketAttachments;
	}

	public void setTeamAndRemarks(String ticketResolutionTeam, String ticketRemarks) {
		team = ticketResolutionTeam;
		remarks = ticketRemarks;
	}

	public void setRemarks(String ticketRemarks) {
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
		try {
			FranchiseOrderResponseBean refundResponse = ticketClientService.cancelFranchiseOrderPostBilling(createCancelFORequest(item, ticket.getId()),
					generateClientKeyForCancel(ticket.getId(), item.getId()), ticket.getMetadata().getOrderDetails().getOrderId());
			item.getDetails().getOrderDetails().setRefundAmount(refundResponse.getFinalBillAmount());
			setRemarks(String.format(TicketCreateActions.AUTOMATIC_ORDER_CANCEL.getRemarks().toString()));
			item.setAssignedTeam(TicketConstants.CLOSED_TICKET_ASSIGNED_TEAM);
			item.setAssignedAt(new Date());
			item.getDetails().setResolvedRemarks(remarks);
			item.setStatus(TicketStatus.CLOSED);
		} catch (Exception e) {
			_LOGGER.error("something went wrong while cancelling order automatically", e);
			if (e instanceof ValidationException) {
				setRemarks(e.getMessage());
			} else {
				setRemarks("something went wrong while cancelling order automatically, please process it manually" + e.getMessage());
			}
		}

		item.setRemarks(remarks);
		actionDetailsBean.setRemarks(remarks);
		actionDetailsBean.setAttachments(attachments);
		ticketHistoryService.addTicketHistory(ticket.getId(), item.getId(), action, actionDetailsBean);
		return true;
	}

	private String generateClientKeyForCancel(Long ticketId, Long ticketItemId) {
		return TicketConstants.CLIENT_KEY_FOR_CANCEL + "|" + ticketId + "|" + ticketItemId;
	}

	private FranchiseOrderCancelPostBillingRequest createCancelFORequest(TicketItemEntity item, Long id) {
		FranchiseOrderCancelPostBillingRequest request = new FranchiseOrderCancelPostBillingRequest();
		request.setRemarks(remarks);
		return request;
	}
}