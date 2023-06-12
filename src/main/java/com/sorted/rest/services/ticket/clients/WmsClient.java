package com.sorted.rest.services.ticket.clients;

import com.sorted.rest.services.ticket.beans.WhSkuResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Set;

@FeignClient(value = "warehouse", url = "${client.wms.url}")
public interface WmsClient {

	@GetMapping(value = "/api/v1/catalog/bulk-request/store-sku")
	List<WhSkuResponse> getStoreSkuInventoryForBulkRequest(@RequestHeader Map<String, Object> headers, @RequestParam Set<String> skuCodes,
			@RequestParam String storeId);

}
