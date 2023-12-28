package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class ImsConsumerOrderRefundItemBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private String skuCode;

	private Double refundQuantity;

	private Boolean warehouseReturnCheck;

	private String refundRemarks;

	public static ImsConsumerOrderRefundItemBean newInstance() {
		return new ImsConsumerOrderRefundItemBean();
	}

}