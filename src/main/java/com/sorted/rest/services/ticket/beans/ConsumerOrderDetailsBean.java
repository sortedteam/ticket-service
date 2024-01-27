package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
public class ConsumerOrderDetailsBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private UUID orderId;

	private String displayOrderId;

	private String orderStatus;

	private Double finalOrderBillAmount;

	private Date deliveryDate;

	private String deliverySlot;

	private Double totalRefundableAmount;

	private Double totalRefundAmount;

	private ConsumerOrderContactDetail contactDetail;

	public static ConsumerOrderDetailsBean newInstance() {
		return new ConsumerOrderDetailsBean();
	}
}
