package com.sorted.rest.services.ticket.clients;

import com.sorted.rest.services.ticket.beans.StoreReturnResponseBean;
import com.sorted.rest.services.ticket.beans.WhSkuResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Set;

@FeignClient(value = "warehouse", url = "${client.wms.url}")
public interface TicketWmsClient {

	@GetMapping(value = "/api/v1/catalog/bulk-request/store-sku")
	List<WhSkuResponse> getStoreSkuInventoryForBulkRequest(@RequestHeader Map<String, Object> headers, @RequestParam Set<String> skuCodes,
			@RequestParam String storeId, @RequestParam Boolean skipPriceOverride);

	@GetMapping("/api/v1/storeReturns/order-id/{id}")
	StoreReturnResponseBean getStoreReturnByOrderId(@RequestHeader Map<String, Object> headers, @PathVariable String id);

}
