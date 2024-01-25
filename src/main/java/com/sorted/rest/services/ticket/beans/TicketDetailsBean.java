package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@ApiModel(description = "Ticket Details Bean")
@Data
public class TicketDetailsBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private String description;

	private List<String> tags;

	private OrderDetailsRequestBean orderDetails;

	private ConsumerOrderDetailsRequestBean consumerOrderDetails;

	//	todo: tickets for PAYMENT_ISSUE with referenceId not allowed in V1, add in subsequent releases
	//	private PaymentDetailsRequestBean paymentDetails;

	public static TicketDetailsBean newInstance() {
		return new TicketDetailsBean();
	}
}
