package com.sorted.rest.services.ticket.clients;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "warehouse", url = "${client.wms.url}")
public interface TicketWmsClient {

}
