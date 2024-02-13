package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@ApiModel(description = "Ticket Details Bean")
@Data
public class TicketMetadataBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private OrderDetailsBean orderDetails;

	private StoreDetailsBean storeDetails;

	private UserDetail consumerDetails;

	private ConsumerOrderDetailsBean consumerOrderDetails;

	public static TicketMetadataBean newInstance() {
		return new TicketMetadataBean();
	}
}
