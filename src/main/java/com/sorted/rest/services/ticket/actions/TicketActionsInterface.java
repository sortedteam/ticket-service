package com.sorted.rest.services.ticket.actions;

import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import org.springframework.stereotype.Component;

@Component
public interface TicketActionsInterface {

	Boolean isApplicable(TicketItemEntity item, Long ticketId, String action, TicketActionDetailsBean actionDetailsBean);

	Boolean apply(TicketItemEntity item, Long ticketId, String action, TicketActionDetailsBean actionDetailsBean);
}