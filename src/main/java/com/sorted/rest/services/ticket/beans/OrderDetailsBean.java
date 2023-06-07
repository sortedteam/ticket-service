package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.sql.Date;

@ApiModel(description = "Resolution Details Bean")
@Data
public class OrderDetailsBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private String skuCode;

	private String orderId;

	private String orderItemId;

	private Date deliveryDate;

	private String deliverySlot;

	private String uom;

	private Double orderedQuantity;

	private Double deliveredQuantity;

	private Double issueQuantity;

	private Double resolvedQuantity;

	public static OrderDetailsBean newInstance() {
		return new OrderDetailsBean();
	}
}
