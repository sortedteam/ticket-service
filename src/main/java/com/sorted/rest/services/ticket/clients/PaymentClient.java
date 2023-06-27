package com.sorted.rest.services.ticket.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.ticket.beans.WalletStatementBean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "payment", url = "${client.payment.url}", configuration = { FeignCustomConfiguration.class })
public interface PaymentClient {

	@GetMapping("/payments/walletStatement/id/{id}")
	WalletStatementBean fetchWalletStatementById(@PathVariable Integer id);

}