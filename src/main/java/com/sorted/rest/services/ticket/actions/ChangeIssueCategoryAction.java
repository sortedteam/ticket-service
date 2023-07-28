package com.sorted.rest.services.ticket.actions;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketStatus;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketUpdateActions;
import com.sorted.rest.services.ticket.entity.TicketCategoryEntity;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.services.TicketCategoryService;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChangeIssueCategoryAction implements TicketActionsInterface {

	static AppLogger _LOGGER = LoggingManager.getLogger(ChangeIssueCategoryAction.class);

	@Autowired
	private TicketHistoryService ticketHistoryService;

	@Autowired
	private TicketCategoryService ticketCategoryService;

	private String remarks;

	private List<String> attachments;

	private Integer categoryLeafId;

	public void setAttachments(List<String> ticketAttachments) {
		attachments = ticketAttachments;
	}

	public void setRemarks(String ticketRemarks) {
		remarks = ticketRemarks;
	}

	public void setCategoryLeafId(Integer ticketCategoryLeafId) {
		categoryLeafId = ticketCategoryLeafId;
	}

	@Override
	public Boolean isApplicable(TicketItemEntity item, TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		if (item.getStatus().equals(TicketStatus.IN_PROGRESS)) {
			if (categoryLeafId == null) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "New issue category not provided", null));
			}
			TicketCategoryEntity categoryLeaf = ticketCategoryService.findRecordById(categoryLeafId);
			if (categoryLeaf == null) {
				throw new ValidationException(
						ErrorBean.withError(Errors.INVALID_REQUEST, String.format("No data found for ticket category with id : %s", categoryLeafId), null));
			} else if (categoryLeaf.getIsTerminal() != 1 || !item.getCategoryLeaf().getOnCreateActions().containsAll(categoryLeaf.getOnCreateActions())) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
						String.format("Cannot change issue category to %s from %s", categoryLeaf.getDescription(), item.getCategoryLeaf().getDescription()),
						null));
			}
			item.setCategoryLeaf(categoryLeaf);
			item.setPriority(item.getCategoryLeaf().getPriority());
			return true;
		}
		return false;
	}

	@Override
	public Boolean apply(TicketItemEntity item, TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean) {
		setRemarks(String.format(TicketUpdateActions.CHANGE_ISSUE_CATEGORY.getRemarks(), remarks));
		item.setRemarks(remarks);
		actionDetailsBean.setRemarks(remarks);
		actionDetailsBean.setAttachments(attachments);
		ticketHistoryService.addTicketHistory(ticket.getId(), item.getId(), action, actionDetailsBean);
		return true;
	}
}