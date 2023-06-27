package com.sorted.rest.services.ticket.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "store", url = "${client.store.url}", configuration = { FeignCustomConfiguration.class })
public interface TicketStoreClient {

}