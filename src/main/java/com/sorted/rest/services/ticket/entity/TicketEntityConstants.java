package com.sorted.rest.services.ticket.entity;

public interface TicketEntityConstants {

	interface Query {

		String GET_TICKET_REFERENCE_ID_WITH_LIMIT_OFFSET = "select t.referenceId from tickets.tickets t where t.status in :statuses and t.categoryRoot in :categoryRoots and t.active = :active group by t.referenceId order by max(t.createdAt) desc limit :limitValue offset :offsetValue";
	}
}