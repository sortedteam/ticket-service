package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@ApiModel(description = "Payment Details Request Bean")
@Data
public class PaymentDetailsRequestBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	public static PaymentDetailsRequestBean newInstance() {
		return new PaymentDetailsRequestBean();
	}
}
