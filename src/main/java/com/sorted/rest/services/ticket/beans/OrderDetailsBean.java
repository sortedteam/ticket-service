package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@ApiModel(description = "Order Details Bean")
@Data
public class OrderDetailsBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private Integer whId;

	private String skuCode;

	private UUID orderId;

	private Date deliveryDate;

	private String deliverySlot;

	private String uom;

	private Double orderedQty;

	private Double deliveredQty;

	private Double issueQty; //

	private Double refundableQty;

	private Double returnQty;

	private String returnRemarks;

	private Double resolvedQty; //

	public static OrderDetailsBean newInstance() {
		return new OrderDetailsBean();
	}
}
