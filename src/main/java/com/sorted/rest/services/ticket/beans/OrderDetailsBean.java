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

	private UUID orderId;

	private String displayOrderId;

	private String orderStatus;

	private Double finalOrderBillAmount;

	private String challanUrl;

	private Date deliveryDate;

	private String deliverySlot;

	private Double totalRefundableAmount;

	private Double totalRefundAmount;

	public static OrderDetailsBean newInstance() {
		return new OrderDetailsBean();
	}
}
