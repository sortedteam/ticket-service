package com.sorted.rest.services.ticket.actions;

import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import org.springframework.stereotype.Component;

@Component
public interface TicketActionsInterface {

	Boolean isApplicable(TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean);

	Boolean apply(TicketEntity ticket, String action, TicketActionDetailsBean actionDetailsBean);
}