package com.sorted.rest.services.ticket.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "widget", url = "${client.widget.url}", configuration = { FeignCustomConfiguration.class })
public interface WidgetClient {

	@GetMapping(value = "/widgets/audiences/filtered")
	String getFilteredAudience(@RequestParam Integer usertype, @RequestParam String entityType, @RequestParam String entityId,
			@RequestParam List<String> superset);

}