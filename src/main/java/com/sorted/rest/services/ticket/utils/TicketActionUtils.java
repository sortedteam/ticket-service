package com.sorted.rest.services.ticket.utils;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.services.ticket.actions.AutomaticOrderRefundAction;
import com.sorted.rest.services.ticket.actions.EscalateToCustomercareAction;
import com.sorted.rest.services.ticket.actions.EscalateToWarehouseAction;
import com.sorted.rest.services.ticket.actions.TicketActionsInterface;
import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.beans.UserDetail;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TicketActionUtils {

	static AppLogger _LOGGER = LoggingManager.getLogger(TicketActionUtils.class);

	@Autowired
	private UserUtils userUtils;

	@Value("${auth.id}")
	private UUID internalAuthUserId;

	@Autowired
	private EscalateToCustomercareAction escalateToCustomercareAction;

	@Autowired
	private EscalateToWarehouseAction escalateToWarehouseAction;

	@Autowired
	private AutomaticOrderRefundAction automaticOrderRefundAction;

	public void invokeTicketDraftAction(TicketEntity ticket) {
		TicketActionDetailsBean defaultActionDetails = TicketActionDetailsBean.newInstance();
		defaultActionDetails.setUserDetail(getInternalAuthUserDetails());
		executeDefaultAction(ticket, defaultActionDetails);
	}

	private void executeDefaultAction(TicketEntity ticket, TicketActionDetailsBean actionDetailsBean) {
		String action = TicketConstants.ESCALATE_TO_CUSTOMERCARE_ACTION;
		TicketActionsInterface ticketAction = escalateToCustomercareAction;
		try {
			if (ticketAction.isApplicable(ticket, action, actionDetailsBean)) {
				ticketAction.apply(ticket, action, TicketActionDetailsBean.newInstance());
			}
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while executing ticketAction : %s on ticket : %s ", action, ticketAction), e);
		}
	}

	public void invokeTicketRaiseAction(TicketEntity ticket, List<String> actions) {
		Boolean terminate = false;
		TicketActionDetailsBean defaultActionDetails = TicketActionDetailsBean.newInstance();
		defaultActionDetails.setUserDetail(getInternalAuthUserDetails());
		for (String action : actions) {
			TicketActionsInterface ticketAction = null;
			if (action.equals(TicketConstants.AUTOMATIC_ORDER_REFUND_ACTION)) {
				ticketAction = automaticOrderRefundAction;
			} else if (action.equals(TicketConstants.ESCALATE_TO_WAREHOUSE_ACTION)) {
				ticketAction = escalateToWarehouseAction;
			} else {
				_LOGGER.info(String.format("Invalid ticketAction : %s ", action));
				continue;
			}
			try {
				if (ticketAction.isApplicable(ticket, action, defaultActionDetails)) {
					if (ticketAction.apply(ticket, action, defaultActionDetails)) {
						terminate = true;
						break;
					}
				}
			} catch (Exception e) {
				_LOGGER.error(String.format("Error while executing ticketAction : %s on ticket : %s ", action, ticketAction), e);
			}
		}
		if (!terminate) {
			executeDefaultAction(ticket, defaultActionDetails);
		}
	}

	private UserDetail getInternalAuthUserDetails() {
		return userUtils.getUserDetail(internalAuthUserId);
	}
}
