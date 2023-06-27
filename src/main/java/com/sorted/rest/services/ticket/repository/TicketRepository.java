package com.sorted.rest.services.ticket.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The Interface OrderRepository.
 */
@Repository
public interface TicketRepository extends BaseCrudRepository<TicketEntity, Long> {

	@Query("select t from TicketEntity t " + "where t.referenceId = ?1 and t.isClosed in ?2 and t.hasDraft in ?3 and t.active = ?4")
	List<TicketEntity> findByReferenceIdAndIsClosedInAndHasDraftInAndActive(String referenceId, List<Integer> isClosed, List<Integer> hasDraft, Integer active);

}