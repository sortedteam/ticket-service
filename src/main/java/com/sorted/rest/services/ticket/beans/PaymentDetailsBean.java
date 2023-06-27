package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@ApiModel(description = "Payment Details Bean")
@Data
public class PaymentDetailsBean extends WalletStatementBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	public static PaymentDetailsBean newInstance() {
		return new PaymentDetailsBean();
	}
}
