package com.sorted.rest.services.ticket.clients;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.services.ticket.beans.UserServiceResponse;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ClientService {

	AppLogger _LOGGER = LoggingManager.getLogger(ClientService.class);

	@Autowired
	private StoreClient storeClient;

	@Autowired
	private AuthConsumerClient authConsumerClient;

	@Autowired
	private OrderClient orderClient;

	@Autowired
	private PaymentClient paymentClient;

	@Autowired
	private WmsClient wmsClient;

	@Autowired
	private WidgetClient widgetClient;

	@Value("${client.wms.auth_key}")
	@Getter
	private String RZ_AUTH_VALUE;

	public UserServiceResponse getUserDetailsFromCustomerId(UUID customerId) {
		String customerIdString = customerId.toString();
		Map<String, Object> headerMap = new HashMap<>();
		try {
			return authConsumerClient.getUserDetailsFromCustomerId(headerMap, customerIdString);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while getting userDetails for customerId  ", customerIdString), e);
			return null;
		}
	}

	public String getFilteredOrDefaultAudience(String entityType, String entityId, List<String> superset, String defaultAudience) {
		String audience = null;
		try {
			audience = widgetClient.getFilteredAudience(TicketConstants.TICKET_RAISING_USER_USERTYPE, entityType, entityId, superset);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while fetching filteredOrDefault audience for %s : %s from subset : %s with default : %s", entityType, entityId,
					superset, defaultAudience), e);
		}
		return audience == null ? defaultAudience : audience;
	}
}
