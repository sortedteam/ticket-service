package com.sorted.rest.services.ticket.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.ticket.beans.TargetCashbackCronRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "offer", url = "${client.offer.url}", configuration = { FeignCustomConfiguration.class })
public interface TicketOfferClient {

	@PostMapping(value = "/offers/targets/cron/cashback/daily")
	void runDailyCashbackCronForStoreIdAndDate(@RequestBody TargetCashbackCronRequest request);

	@PostMapping("/offers/targets/cron/cashback/weekly")
	void runWeeklyCashbackCronForStoreIdAndDate(@RequestBody TargetCashbackCronRequest request);
}