package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@ApiModel(description = "Ticket Details Bean")
@Data
public class TicketDetailsBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private String description;

	private OrderDetailsBean orderDetails;

	private PaymentDetailsBean paymentDetails;

	public static TicketDetailsBean newInstance() {
		return new TicketDetailsBean();
	}
}
