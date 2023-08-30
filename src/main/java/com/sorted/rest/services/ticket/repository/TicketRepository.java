package com.sorted.rest.services.ticket.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * The Interface OrderRepository.
 */
@Repository
public interface TicketRepository extends BaseCrudRepository<TicketEntity, Long> {

	@Query(value = "SELECT t.* FROM ticket.tickets t JOIN ticket.ticket_items ti ON ti.ticket_id = t.id JOIN ticket.ticket_categories lc ON ti.category_leaf_id = lc.id WHERE t.active = 1 AND ti.active = 1 AND t.requester_entity_id = COALESCE(CAST(:requesterEntityId AS varchar), t.requester_entity_id) AND t.requester_entity_category = COALESCE(CAST(:requesterEntityCategory AS varchar), t.requester_entity_category) AND t.last_added_at >= :fromDate AND t.last_added_at <= :toDate AND t.category_root_id in :categoryRootsIn AND (CASE WHEN :hasDraft IS NULL THEN true WHEN CAST(CAST(:hasDraft as varchar) as BOOLEAN) THEN (t.draft_count > 0) ELSE (t.draft_count = 0) END) = true AND (CASE WHEN :hasPending IS NULL THEN (t.pending_count >= 0) WHEN CAST(CAST(:hasPending as varchar) as BOOLEAN) THEN (t.pending_count > 0) ELSE (t.pending_count = 0) END) = true AND (CASE WHEN :hasClosed IS NULL THEN (t.closed_count >= 0) WHEN CAST(CAST(:hasClosed as varchar) as BOOLEAN) THEN (t.closed_count > 0) ELSE (t.closed_count = 0) END) = true AND (lc.id = :categoryLeafParentId OR lc.parent_id = :categoryLeafParentId)", nativeQuery = true)
	List<TicketEntity> findCustomWithCategoryLeafFilter(String requesterEntityId, String requesterEntityCategory, Date fromDate, Date toDate, Boolean hasDraft,
			Boolean hasPending, Boolean hasClosed, List<Integer> categoryRootsIn, Integer categoryLeafParentId);

	@Query(value = "SELECT distinct t.reference_id FROM ticket.tickets t JOIN ticket.ticket_items ti ON ti.ticket_id = t.id WHERE t.active = 1 AND ti.active = 1 and ti.status = 'IN_PROGRESS' and t.pending_count > 0 and t.reference_id in :orderIds and ti.details->>'orderDetails' is not null and ti.details->'orderDetails'->>'isReturnIssue'='true'", nativeQuery = true)
	List<String> getPendingTickets(List<String> orderIds);
}