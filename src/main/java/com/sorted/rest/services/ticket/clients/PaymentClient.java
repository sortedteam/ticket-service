package com.sorted.rest.services.ticket.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.ticket.beans.WalletStatementBean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(value = "payment", url = "${client.payment.url}", configuration = { FeignCustomConfiguration.class })
public interface PaymentClient {

	@GetMapping("/payments/walletStatement/{txnDetail}")
	List<WalletStatementBean> fetchWalletStatementByTxnDetail(@PathVariable String txnDetail);

}