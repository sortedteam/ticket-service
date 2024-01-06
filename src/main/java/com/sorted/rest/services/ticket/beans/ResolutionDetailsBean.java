package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@ApiModel(description = "Resolution Details Bean")
@Data
public class ResolutionDetailsBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private String description;

	private String resolvedRemarks;

	private UserDetail resolvedBy;

	private OrderItemDetailsBean orderDetails;

	private ConsumerOrderItemDetailsBean consumerOrderDetails;

	//	todo: tickets for PAYMENT_ISSUE with referenceId not allowed in V1, add in subsequent releases
	//	private PaymentDetailsBean paymentDetails;

	public static ResolutionDetailsBean newInstance() {
		return new ResolutionDetailsBean();
	}
}
