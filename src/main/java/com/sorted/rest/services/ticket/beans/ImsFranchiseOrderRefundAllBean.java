package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class ImsFranchiseOrderRefundAllBean implements Serializable {

	private static final long serialVersionUID = 4143679869385600722L;

	private UUID parentOrderId;

	private Long ticketId;

	private Long ticketItemId;

	private Boolean warehouseReturnCheck;

	private String refundRemarks;

	public static ImsFranchiseOrderRefundAllBean newInstance() {
		return new ImsFranchiseOrderRefundAllBean();
	}
}