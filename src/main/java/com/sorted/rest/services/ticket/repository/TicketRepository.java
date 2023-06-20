package com.sorted.rest.services.ticket.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * The Interface OrderRepository.
 */
@Repository
public interface TicketRepository extends BaseCrudRepository<TicketEntity, Long> {

	List<TicketEntity> findByReferenceIdAndStatusInAndActive(String referenceId, Collection<String> statuses, Integer active);

	@Query(value = "select * from (select reference_id from ticket.tickets where reference_id is not null and status in :statuses and active = :active group by reference_id order by max(created_at) desc limit :limitValue offset :offsetValue)", nativeQuery = true)
	PageAndSortResult<String> findGroupedByReferenceId(List<String> statuses, int limitValue, int offsetValue, int active);

}