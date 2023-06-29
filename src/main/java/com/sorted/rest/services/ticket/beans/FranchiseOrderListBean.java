package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
public class FranchiseOrderListBean implements Serializable {

	private static final long serialVersionUID = -4464420110328768366L;

	private UUID id;

	private String displayOrderId;

	private String status;

	private Double finalBillAmount;

	private String challanUrl;

	private UUID parentOrderId;

	private Date deliveryDate;

	private String storeId;

	private String slot;

	public static FranchiseOrderListBean newInstance() {
		return new FranchiseOrderListBean();
	}
}