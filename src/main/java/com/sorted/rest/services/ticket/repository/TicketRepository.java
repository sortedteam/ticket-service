package com.sorted.rest.services.ticket.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import org.springframework.stereotype.Repository;

/**
 * The Interface OrderRepository.
 */
@Repository
public interface TicketRepository extends BaseCrudRepository<TicketEntity, Long> {

}