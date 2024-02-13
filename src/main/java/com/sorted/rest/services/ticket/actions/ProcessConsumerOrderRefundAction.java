package com.sorted.rest.services.ticket.actions;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.services.ticket.beans.ConsumerOrderResponseBean;
import com.sorted.rest.services.ticket.beans.ImsConsumerOrderRefundBean;
import com.sorted.rest.services.ticket.beans.ImsConsumerOrderRefundItemBean;
import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.clients.TicketClientService;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketStatus;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketUpdateActions;
import com.sorted.rest.services.ticket.constants.TicketConstants.UserRoles;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class ProcessConsumerOrderRefundAction implements TicketActionsInterface {

	static AppLogger _LOGGER = LoggingManager.getLogger(ProcessConsumerOrderRefundAction.class);

	@Autowired
	private TicketHistoryService ticketHistoryService;

	@Autowired
	private TicketClientService ticketClientService;

	private String remarks;

	private Double resolvedQuantity;

	private List<String> attachments;

	public void setAttachments(List<String> ticketAttachments) {
		attachments = ticketAttachments;
	}

	public void setRemarks(String ticketRemarks) {
		remarks = ticketRemarks;
	}

	public void setResolvedQuantity(Double quantity) {
		if (quantity == null || quantity.compareTo(0d) != 1) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Refund Order can not be processed with quantity less than zero", null));
		}
		resolvedQuantity = quantity;
	}

	@Override
	public Boolean isApplicable(TicketItemEntity item, TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		if (!SessionUtils.getAuthUserRoles().contains(UserRoles.CCMANAGER.toString())) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Refund Order can only be processed by Customer Care Manager.", null));
		}
		return item.getDetails().getConsumerOrderDetails() != null && item.getDetails().getConsumerOrderDetails().getOrderId() != null && item.getDetails()
				.getConsumerOrderDetails().getSkuCode() != null && item.getDetails().getConsumerOrderDetails().getIssueQty() != null && item.getDetails()
				.getConsumerOrderDetails().getUom() != null;
	}

	@Override
	public Boolean apply(TicketItemEntity item, TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		ConsumerOrderResponseBean refundResponse = ticketClientService.imsProcessConsumerRefundOrder(createRefundBean(item, ticket.getId()),
				generateClientKeyForRefund(ticket.getId(), item.getId()));
		item.getDetails().getConsumerOrderDetails().setRefundAmount(refundResponse.getFinalBillAmount());
		item.getDetails().getConsumerOrderDetails().setResolvedQty(resolvedQuantity);
		setRemarks(String.format(TicketUpdateActions.PROCESS_CONSUMER_ORDER_REFUND.getRemarks(), remarks));
		item.setAssignedTeam(TicketConstants.CLOSED_TICKET_ASSIGNED_TEAM);
		item.setAssignedAt(new Date());
		item.setRemarks(remarks);
		item.getDetails().setResolvedRemarks(remarks);
		item.getDetails().setResolvedBy(actionDetailsBean.getUserDetail());
		item.setStatus(TicketStatus.CLOSED);

		if (ticket.getMetadata().getConsumerOrderDetails() != null) {
			ticket.getMetadata().getConsumerOrderDetails().setTotalRefundAmount(BigDecimal.valueOf(
					ticket.getMetadata().getConsumerOrderDetails().getTotalRefundAmount() != null ?
							ticket.getMetadata().getConsumerOrderDetails().getTotalRefundAmount() :
							0d).add(BigDecimal.valueOf(item.getDetails().getConsumerOrderDetails().getRefundAmount())).doubleValue());
		}

		actionDetailsBean.setRemarks(remarks);
		actionDetailsBean.setAttachments(attachments);
		ticketHistoryService.addTicketHistory(ticket.getId(), item.getId(), action, actionDetailsBean);
		return true;
	}

	private ImsConsumerOrderRefundBean createRefundBean(TicketItemEntity item, Long ticketId) {
		ImsConsumerOrderRefundItemBean imsConsumerOrderRefundItemBean = ImsConsumerOrderRefundItemBean.newInstance();
		imsConsumerOrderRefundItemBean.setSkuCode(item.getDetails().getConsumerOrderDetails().getSkuCode());
		imsConsumerOrderRefundItemBean.setRefundQuantity(resolvedQuantity);
		ImsConsumerOrderRefundBean imsConsumerOrderRefundBean = ImsConsumerOrderRefundBean.newInstance();
		imsConsumerOrderRefundBean.setTicketId(ticketId);
		imsConsumerOrderRefundBean.setTicketItemId(item.getId());
		imsConsumerOrderRefundBean.setParentOrderId(item.getDetails().getConsumerOrderDetails().getOrderId());
		imsConsumerOrderRefundBean.setRefundOrderItems(Collections.singletonList(imsConsumerOrderRefundItemBean));
		return imsConsumerOrderRefundBean;
	}

	private String generateClientKeyForRefund(Long ticketId, Long ticketItemId) {
		return TicketConstants.CLIENT_KEY_FOR_REFUND + "|" + ticketId + "|" + ticketItemId;
	}
}