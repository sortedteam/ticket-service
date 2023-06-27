package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class StoreDetailsBean implements Serializable {

	private static final long serialVersionUID = 493517626137997750L;

	private String name;

	private String storeId;

	private String contactNumber;

	private String ownerName;

	private String status;

	private Boolean isActive;

	private BigDecimal refundPermissibilityFactor;

	public static StoreDetailsBean newInstance() {
		return new StoreDetailsBean();
	}
}
