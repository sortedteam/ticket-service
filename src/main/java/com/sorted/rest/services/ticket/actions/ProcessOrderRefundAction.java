package com.sorted.rest.services.ticket.actions;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.services.ticket.beans.FranchiseOrderResponseBean;
import com.sorted.rest.services.ticket.beans.ImsFranchiseOrderRefundBean;
import com.sorted.rest.services.ticket.beans.ImsFranchiseOrderRefundItemBean;
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
public class ProcessOrderRefundAction implements TicketActionsInterface {

	static AppLogger _LOGGER = LoggingManager.getLogger(ProcessOrderRefundAction.class);

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
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Refund Order can only be processed by Partner Care Manager.", null));
		}
		return item.getDetails().getOrderDetails() != null && item.getDetails().getOrderDetails().getOrderId() != null && item.getDetails().getOrderDetails()
				.getSkuCode() != null && item.getDetails().getOrderDetails().getIssueQty() != null && item.getDetails().getOrderDetails().getUom() != null;
	}

	@Override
	public Boolean apply(TicketItemEntity item, TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		FranchiseOrderResponseBean refundResponse = ticketClientService.imsProcessFranchiseRefundOrder(createRefundBean(item, ticket.getId()),
				generateClientKeyForRefund(ticket.getId(), item.getId()));
		item.getDetails().getOrderDetails().setRefundAmount(refundResponse.getFinalBillAmount());
		item.getDetails().getOrderDetails().setResolvedQty(resolvedQuantity);
		setRemarks(String.format(TicketUpdateActions.PROCESS_ORDER_REFUND.getRemarks(), resolvedQuantity, item.getDetails().getOrderDetails().getIssueQty(),
				item.getDetails().getOrderDetails().getUom(), remarks));
		item.setAssignedTeam(TicketConstants.CLOSED_TICKET_ASSIGNED_TEAM);
		item.setAssignedAt(new Date());
		item.setRemarks(remarks);
		item.getDetails().setResolvedRemarks(remarks);
		item.getDetails().setResolvedBy(actionDetailsBean.getUserDetail());
		item.setStatus(TicketStatus.CLOSED);

		if (ticket.getMetadata().getOrderDetails() != null) {
			ticket.getMetadata().getOrderDetails().setTotalRefundAmount(BigDecimal.valueOf(
							ticket.getMetadata().getOrderDetails().getTotalRefundAmount() != null ? ticket.getMetadata().getOrderDetails().getTotalRefundAmount() : 0d)
					.add(BigDecimal.valueOf(item.getDetails().getOrderDetails().getRefundAmount())).doubleValue());
		}

		actionDetailsBean.setRemarks(remarks);
		actionDetailsBean.setAttachments(attachments);
		ticketHistoryService.addTicketHistory(ticket.getId(), item.getId(), action, actionDetailsBean);
		return true;
	}

	private ImsFranchiseOrderRefundBean createRefundBean(TicketItemEntity item, Long ticketId) {
		ImsFranchiseOrderRefundItemBean franchiseOrderRefundItemBean = ImsFranchiseOrderRefundItemBean.newInstance();
		franchiseOrderRefundItemBean.setSkuCode(item.getDetails().getOrderDetails().getSkuCode());
		franchiseOrderRefundItemBean.setRefundQuantity(resolvedQuantity);
		franchiseOrderRefundItemBean.setWarehouseReturnCheck(
				item.getDetails().getOrderDetails().getReturnQty() != null && item.getDetails().getOrderDetails().getReturnQty().compareTo(0d) == 1);
		franchiseOrderRefundItemBean.setRefundRemarks(remarks);

		ImsFranchiseOrderRefundBean franchiseOrderRefundBean = ImsFranchiseOrderRefundBean.newInstance();
		franchiseOrderRefundBean.setTicketId(ticketId);
		franchiseOrderRefundBean.setTicketItemId(item.getId());
		franchiseOrderRefundBean.setParentOrderId(item.getDetails().getOrderDetails().getOrderId());
		franchiseOrderRefundBean.setRefundOrderItems(Collections.singletonList(franchiseOrderRefundItemBean));
		return franchiseOrderRefundBean;
	}

	private String generateClientKeyForRefund(Long ticketId, Long ticketItemId) {
		return TicketConstants.CLIENT_KEY_FOR_REFUND + "|" + ticketId + "|" + ticketItemId;
	}
}