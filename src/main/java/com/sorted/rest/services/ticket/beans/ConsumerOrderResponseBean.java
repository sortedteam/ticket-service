package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@ApiModel(description = "Franchise Order Response Bean extending the List Bean")
@Data
public class ConsumerOrderResponseBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private UUID id;

	private String displayOrderId;

	private String status;

	private Double finalBillAmount;

	public static ConsumerOrderResponseBean newInstance() {
		return new ConsumerOrderResponseBean();
	}
}
