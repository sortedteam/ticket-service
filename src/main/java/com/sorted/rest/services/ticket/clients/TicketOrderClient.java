package com.sorted.rest.services.ticket.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.ticket.beans.FranchiseOrderListBean;
import com.sorted.rest.services.ticket.beans.FranchiseOrderResponseBean;
import com.sorted.rest.services.ticket.beans.ImsFranchiseOrderRefundBean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@FeignClient(value = "order", url = "${client.order.url}", configuration = { FeignCustomConfiguration.class })
public interface TicketOrderClient {

	@GetMapping("/orders/franchise/{orderId}")
	FranchiseOrderResponseBean getFranchiseOrderInfo(@RequestHeader Map<String, Object> headerMap, @PathVariable UUID orderId);

	@GetMapping("/orders/franchise/display-ids")
	List<FranchiseOrderListBean> getFranchiseOrderByDisplayIds(@RequestParam Set<String> ids);

	@PostMapping("/orders/franchise/ims/refund")
	FranchiseOrderResponseBean imsProcessFranchiseRefundOrder(@RequestBody ImsFranchiseOrderRefundBean request);

}