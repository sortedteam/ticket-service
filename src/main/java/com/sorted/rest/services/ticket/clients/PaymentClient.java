package com.sorted.rest.services.ticket.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "payment", url = "${client.payment.url}", configuration = { FeignCustomConfiguration.class })
public interface PaymentClient {

}