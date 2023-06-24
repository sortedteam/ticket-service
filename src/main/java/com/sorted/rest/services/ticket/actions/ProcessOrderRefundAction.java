package com.sorted.rest.services.ticket.actions;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.services.ticket.beans.ImsFranchiseOrderRefundBean;
import com.sorted.rest.services.ticket.beans.ImsFranchiseOrderRefundItemBean;
import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.clients.ClientService;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketStatus;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketUpdateActions;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;

@Component
public class ProcessOrderRefundAction implements TicketActionsInterface {

	static AppLogger _LOGGER = LoggingManager.getLogger(ProcessOrderRefundAction.class);

	@Autowired
	private TicketHistoryService ticketHistoryService;

	@Autowired
	private ClientService clientService;

	private String remarks;

	private Double resolvedQuantity;

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
	public Boolean isApplicable(TicketItemEntity item, Long ticketId, String action, TicketActionDetailsBean actionDetailsBean) {
		return item.getResolutionDetails().getOrderDetails() != null && item.getResolutionDetails().getOrderDetails()
				.getOrderId() != null && item.getResolutionDetails().getOrderDetails().getSkuCode() != null && item.getResolutionDetails().getOrderDetails()
				.getIssueQty() != null && item.getResolutionDetails().getOrderDetails().getUom() != null;
	}

	@Override
	public Boolean apply(TicketItemEntity item, Long ticketId, String action, TicketActionDetailsBean actionDetailsBean) {
		clientService.imsProcessFranchiseRefundOrder(createRefundBean(item, ticketId));
		setRemarks(String.format(TicketUpdateActions.PROCESS_ORDER_REFUND.getRemarks(), resolvedQuantity,
				item.getResolutionDetails().getOrderDetails().getIssueQty(), item.getResolutionDetails().getOrderDetails().getUom()));
		item.setAssignedTeam(TicketConstants.CLOSED_TICKET_ASSIGNED_TEAM);
		item.setAssignedAt(new Date());
		item.setRemarks(remarks);
		item.setStatus(TicketStatus.CLOSED.toString());
		actionDetailsBean.setRemarks(remarks);
		ticketHistoryService.addTicketHistory(ticketId, item.getId(), action, actionDetailsBean);
		return true;
	}

	private ImsFranchiseOrderRefundBean createRefundBean(TicketItemEntity item, Long ticketId) {
		ImsFranchiseOrderRefundItemBean franchiseOrderRefundItemBean = ImsFranchiseOrderRefundItemBean.newInstance();
		franchiseOrderRefundItemBean.setSkuCode(item.getResolutionDetails().getOrderDetails().getSkuCode());
		franchiseOrderRefundItemBean.setRefundQuantity(resolvedQuantity);
		franchiseOrderRefundItemBean.setWarehouseReturnCheck(
				item.getResolutionDetails().getOrderDetails().getReturnQty() != null && item.getResolutionDetails().getOrderDetails().getReturnQty()
						.compareTo(0d) == 1);
		franchiseOrderRefundItemBean.setRefundRemarks(remarks);

		ImsFranchiseOrderRefundBean franchiseOrderRefundBean = ImsFranchiseOrderRefundBean.newInstance();
		franchiseOrderRefundBean.setTicketId(ticketId);
		franchiseOrderRefundBean.setTicketItemId(item.getId());
		franchiseOrderRefundBean.setParentOrderId(item.getResolutionDetails().getOrderDetails().getOrderId());
		franchiseOrderRefundBean.setRefundOrderItems(Collections.singletonList(franchiseOrderRefundItemBean));
		return franchiseOrderRefundBean;
	}
}