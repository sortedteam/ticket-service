package com.sorted.rest.services.ticket.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.ticket.constants.TicketConstants.EntityType;
import com.sorted.rest.services.ticket.entity.TicketCategoryEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The Interface OrderRepository.
 */
@Repository
public interface TicketCategoryRepository extends BaseCrudRepository<TicketCategoryEntity, Integer> {

	@Query("select t from TicketCategoryEntity t where t.entityType = ?1")
	List<TicketCategoryEntity> findAllByEntityType(EntityType entityType);
}