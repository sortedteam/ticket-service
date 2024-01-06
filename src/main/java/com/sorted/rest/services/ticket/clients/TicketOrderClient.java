package com.sorted.rest.services.ticket.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.ticket.beans.*;
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
	FranchiseOrderResponseBean imsProcessFranchiseRefundOrder(@RequestBody ImsFranchiseOrderRefundBean request, @RequestParam String key);

	@PostMapping("/orders/franchise/ims/refund/all")
	FranchiseOrderResponseBean imsProcessFranchiseRefundAllOrder(@RequestBody ImsFranchiseOrderRefundAllBean request, @RequestParam String key);

	@PostMapping("/orders/franchise/{orderId}/cancel/post-delivery")
	FranchiseOrderResponseBean cancelFranchiseOrderPostBilling(@RequestBody FranchiseOrderCancelPostBillingRequest request, @RequestParam String key,
			@PathVariable UUID orderId);

	@GetMapping("/orders/{id}/internal")
	ConsumerOrderResponseBean getConsumerOrderDetails(@PathVariable UUID id);

	@GetMapping("/orders/display-ids")
	List<ConsumerOrderListBean> getConsumerOrderByDisplayIds(@RequestParam Set<String> ids);

	@PostMapping("/orders/ims/refund")
	ConsumerOrderResponseBean imsConsumerRefundOrder(@RequestBody ImsConsumerOrderRefundBean request, @RequestParam String key);

	@PostMapping("/orders/ims/refund/all")
	ConsumerOrderResponseBean imsConsumerRefundAllOrder(@RequestBody ImsConsumerOrderRefundAllBean request, @RequestParam String key);

}