package com.sorted.rest.services.ticket.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.ticket.beans.UserServiceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(value = "authConsumer", url = "${client.auth.url}", configuration = { FeignCustomConfiguration.class })
public interface TicketAuthConsumerClient {

	@GetMapping(value = "/auth/internal/user/{customerId}")
	UserServiceResponse getUserDetailsFromCustomerId(@RequestHeader Map<String, Object> headers, @PathVariable String customerId);

}