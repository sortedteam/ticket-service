package com.sorted.rest.services.ticket.actions;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketStatus;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketUpdateActions;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Component
public class CancelTicketAction implements TicketActionsInterface {

	static AppLogger _LOGGER = LoggingManager.getLogger(CancelTicketAction.class);

	@Autowired
	private TicketHistoryService ticketHistoryService;

	private String remarks;

	private List<String> attachments;

	public void setAttachments(List<String> ticketAttachments) {
		attachments = ticketAttachments;
	}

	public void setRemarks(String ticketRemarks) {
		remarks = ticketRemarks;
	}

	@Override
	public Boolean isApplicable(TicketItemEntity item, TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		return item.getStatus().equals(TicketStatus.DRAFT) || item.getStatus().equals(TicketStatus.IN_PROGRESS);
	}

	@Override
	public Boolean apply(TicketItemEntity item, TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		setRemarks(String.format(TicketUpdateActions.CANCEL_WITH_REMARKS.getRemarks(), remarks));
		item.setAssignedTeam(TicketConstants.CLOSED_TICKET_ASSIGNED_TEAM);
		item.setAssignedAt(new Date());
		item.setRemarks(remarks);
		item.getDetails().setResolvedRemarks(remarks);
		item.setStatus(TicketStatus.CANCELLED);

		if (item.getDetails().getOrderDetails() != null && item.getDetails().getOrderDetails().getRefundableAmount() != null && ticket.getMetadata()
				.getOrderDetails() != null && ticket.getMetadata().getOrderDetails().getTotalRefundableAmount() != null) {
			ticket.getMetadata().getOrderDetails().setTotalRefundableAmount(
					BigDecimal.valueOf(ticket.getMetadata().getOrderDetails().getTotalRefundableAmount())
							.subtract(BigDecimal.valueOf(item.getDetails().getOrderDetails().getRefundableAmount())).doubleValue());
		}

		actionDetailsBean.setRemarks(remarks);
		actionDetailsBean.setAttachments(attachments);
		ticketHistoryService.addTicketHistory(ticket.getId(), item.getId(), action, actionDetailsBean);
		return true;
	}
}