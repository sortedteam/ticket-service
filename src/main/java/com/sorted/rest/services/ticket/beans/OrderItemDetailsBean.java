package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@ApiModel(description = "Order Item Details Bean")
@Data
public class OrderItemDetailsBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private Integer whId;

	private String skuCode;

	private String productName;

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

	private Double returnQty;

	private Double returnRefundQty;

	private String returnRemarks;

	private String returnQaResult;

	private Double resolvedQty;

	private Double refundAmount;

	private Boolean isReturnIssue = false;

	private Boolean isAutoRefundEligible = false;

	public static OrderItemDetailsBean newInstance() {
		return new OrderItemDetailsBean();
	}
}
