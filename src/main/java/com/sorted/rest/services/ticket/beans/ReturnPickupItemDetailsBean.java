package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@ApiModel(description = "Order Details Request Bean")
@Data
public class ReturnPickupItemDetailsBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private String skuCode;

	private Double deliveredQty;

	private Double issueQty;

	public static ReturnPickupItemDetailsBean newInstance() {
		return new ReturnPickupItemDetailsBean();
	}
}
