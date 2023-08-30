package com.sorted.rest.services.ticket.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.ticket.beans.UserServiceResponse;
import com.sorted.rest.services.ticket.beans.UserStoreMappingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(value = "authConsumer", url = "${client.auth.url}", configuration = { FeignCustomConfiguration.class })
public interface TicketAuthConsumerClient {

	@GetMapping(value = "/auth/internal/user/{customerId}")
	UserServiceResponse getUserDetailsFromCustomerId(@RequestHeader Map<String, Object> headers, @PathVariable String customerId);

	@GetMapping(value = "/auth/internal/user-store-mapping")
	List<UserStoreMappingResponse> getUserStoreMappingFromUserId(@RequestParam(name = "userId") String userId);
}