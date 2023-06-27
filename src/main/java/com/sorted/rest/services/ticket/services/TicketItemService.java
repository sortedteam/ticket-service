package com.sorted.rest.services.ticket.services;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.repository.TicketItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class TicketItemService implements BaseService<TicketItemEntity> {

	@Autowired
	private TicketItemRepository ticketItemRepository;

	AppLogger _LOGGER = LoggingManager.getLogger(TicketItemService.class);

	public TicketItemEntity findById(Long id) {
		Optional<TicketItemEntity> resultOpt = ticketItemRepository.findById(id);
		if (resultOpt.isPresent()) {
			return resultOpt.get();
		}
		return null;
	}

	public List<TicketItemEntity> saveAll(List<TicketItemEntity> ticketItemEntities) {
		Iterable<TicketItemEntity> result = ticketItemRepository.saveAll(ticketItemEntities);
		return StreamSupport.stream(result.spliterator(), false).collect(Collectors.toList());
	}

	@Override
	public Class<TicketItemEntity> getEntity() {
		return TicketItemEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return ticketItemRepository;
	}
}