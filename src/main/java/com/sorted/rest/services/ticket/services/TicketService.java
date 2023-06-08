package com.sorted.rest.services.ticket.services;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.ticket.clients.ClientService;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class TicketService implements BaseService<TicketEntity> {

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private ClientService clientService;

	AppLogger _LOGGER = LoggingManager.getLogger(TicketService.class);

	public TicketEntity findById(Long id) {
		Optional<TicketEntity> resultOpt = ticketRepository.findById(id);
		if (resultOpt.isPresent()) {
			return resultOpt.get();
		}
		return null;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public TicketEntity save(TicketEntity entity) {
		TicketEntity result = ticketRepository.save(entity);
		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<TicketEntity> saveAll(List<TicketEntity> entities) {
		List<TicketEntity> result = StreamSupport.stream(ticketRepository.saveAll(entities).spliterator(), false).collect(Collectors.toList());
		return result;
	}

	@Override
	public Class<TicketEntity> getEntity() {
		return TicketEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return ticketRepository;
	}
}