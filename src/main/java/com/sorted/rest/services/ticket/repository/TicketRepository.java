package com.sorted.rest.services.ticket.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketStatus;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * The Interface OrderRepository.
 */
@Repository
public interface TicketRepository extends BaseCrudRepository<TicketEntity, Long> {

	@Query(value = "SELECT t.* FROM ticket.tickets t JOIN ticket.ticket_items ti ON ti.ticket_id = t.id JOIN ticket.ticket_categories lc ON ti.category_leaf_id = lc.id WHERE t.active = 1 AND ti.active = 1 AND ((CAST(CAST(:skipCheckRequesterEntity as varchar) as BOOLEAN)) OR t.requester_entity_id in :requesterEntityIds) AND t.requester_entity_category = COALESCE(CAST(:requesterEntityCategory AS varchar), t.requester_entity_category) AND ((CAST(CAST(:skipCheckDates as varchar) as BOOLEAN)) OR (t.last_added_at >= :fromDate AND t.last_added_at <= :toDate)) AND t.category_root_id in :categoryRootsIn AND (CASE WHEN :hasDraft IS NULL THEN true WHEN CAST(CAST(:hasDraft as varchar) as BOOLEAN) THEN (t.draft_count > 0) ELSE (t.draft_count = 0) END) = true AND (CASE WHEN :hasPending IS NULL THEN (t.pending_count >= 0) WHEN CAST(CAST(:hasPending as varchar) as BOOLEAN) THEN (t.pending_count > 0) ELSE (t.pending_count = 0) END) = true AND (CASE WHEN :hasClosed IS NULL THEN (t.closed_count >= 0) WHEN CAST(CAST(:hasClosed as varchar) as BOOLEAN) THEN (t.closed_count > 0) ELSE (t.closed_count = 0) END) = true AND (lc.id = :categoryLeafParentId OR lc.parent_id = :categoryLeafParentId)", nativeQuery = true)
	List<TicketEntity> findCustomWithCategoryLeafFilter(Boolean skipCheckRequesterEntity, Boolean skipCheckDates, List<String> requesterEntityIds,
			String requesterEntityCategory, Date fromDate, Date toDate, Boolean hasDraft, Boolean hasPending, Boolean hasClosed, List<Integer> categoryRootsIn,
			Integer categoryLeafParentId);

	@Query(value = "SELECT DISTINCT t FROM TicketEntity t JOIN FETCH t.items ti WHERE t.active = 1 AND ti.active = 1 AND (t.requesterEntityId = :storeId OR :storeId IS NULL) AND ti.createdAt >= :createdFrom AND ti.createdAt <= :createdTo AND t.categoryRootId = :categoryRootId AND t.pendingCount >= 0 AND t.closedCount >= 0 AND ti.status IN :ticketStatuses AND JSON_VALUE(ti.details, '$.orderDetails.skuCode') = COALESCE(:skuCode, JSON_VALUE(ti.details, '$.orderDetails.skuCode')) ORDER BY ti.createdAt")
	List<TicketEntity> findCustomOrderRelated(Date createdFrom, Date createdTo, List<TicketStatus> ticketStatuses, Integer categoryRootId, String storeId,
			String skuCode);

}