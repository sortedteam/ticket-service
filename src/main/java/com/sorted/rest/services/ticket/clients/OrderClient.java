package com.sorted.rest.services.ticket.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.ticket.beans.FranchiseOrderResponseBean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;
import java.util.UUID;

@FeignClient(value = "order", url = "${client.util.url}", configuration = { FeignCustomConfiguration.class })
public interface OrderClient {

	@GetMapping("/orders/franchise/{orderId}")
	FranchiseOrderResponseBean getFranchiseOrderInfo(@RequestHeader Map<String, Object> headerMap, @PathVariable UUID orderId);

}