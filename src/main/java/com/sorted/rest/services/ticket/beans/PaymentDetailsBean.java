package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@ApiModel(description = "Payment Details Bean")
@Data
public class PaymentDetailsBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private String txnDetail;

	private Long walletStatementId;

	private List<WalletStatementBean> walletStatementBeans;

	public static PaymentDetailsBean newInstance() {
		return new PaymentDetailsBean();
	}
}
