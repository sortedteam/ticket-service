package com.sorted.rest.services.ticket.clients;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.services.ticket.beans.UserServiceResponse;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TicketClientService {

	AppLogger _LOGGER = LoggingManager.getLogger(TicketClientService.class);

	@Autowired
	private TicketStoreClient ticketStoreClient;

	@Autowired
	private TicketAuthConsumerClient ticketAuthConsumerClient;

	@Autowired
	private TicketPaymentClient ticketPaymentClient;

	@Autowired
	private TicketWmsClient ticketWmsClient;

	@Value("${client.wms.auth_key}")
	@Getter
	private String RZ_AUTH_VALUE;

	public UserServiceResponse getUserDetailsFromCustomerId(UUID customerId) {
		String customerIdString = customerId.toString();
		Map<String, Object> headerMap = new HashMap<>();
		try {
			return ticketAuthConsumerClient.getUserDetailsFromCustomerId(headerMap, customerIdString);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while getting userDetails for customerId  ", customerIdString), e);
			return null;
		}
	}
}
