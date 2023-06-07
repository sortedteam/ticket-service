package com.sorted.rest.services.ticket.services;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.ticket.entity.TicketHistoryEntity;
import com.sorted.rest.services.ticket.repository.TicketHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by Abhishek on 20.8.22.
 */
@Service
public class TicketHistoryService implements BaseService<TicketHistoryEntity> {

	AppLogger _LOGGER = LoggingManager.getLogger(TicketHistoryService.class);

	@Autowired
	private TicketHistoryRepository ticketHistoryRepository;

	@Transactional(propagation = Propagation.REQUIRED)
	public TicketHistoryEntity save(TicketHistoryEntity entity) {
		TicketHistoryEntity result = ticketHistoryRepository.save(entity);
		return result;
	}

	@Override
	public Class<TicketHistoryEntity> getEntity() {
		return TicketHistoryEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return ticketHistoryRepository;
	}

}