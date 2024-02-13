package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
public class ConsumerOrderItemDetailsBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private String skuCode;

	private String productName;

	private String imageUrl;

	private UUID orderId;

	private Double finalItemAmount;

	private Double prorataAmount;

	private Double refundableAmount;

	private String itemStatus;

	private String uom;

	private Double orderedQty;

	private Double deliveredQty;

	private Double issueQty;

	private Double refundableQty;

	private Double resolvedQty;

	private Double refundAmount;

	private Date refundedAt;

	private Boolean isRefundIssue = false;

	private Boolean isAutoRefundEligible = false;

	private ConsumerOrderItemMetadata metadata;

	public static ConsumerOrderItemDetailsBean newInstance() {
		return new ConsumerOrderItemDetailsBean();
	}
}
