package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@ApiModel(description = "Order Item Response Bean")
@Data
public class ConsumerOrderItemResponseBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private UUID id;

	private UUID orderId;

	private String skuCode;

	private String productName;

	private String uom;

	private String imageUrl;

	private Double orderedQty;

	private Double finalQuantity;

	private Double finalAmount;

	private Integer isRefundable;

	private Integer isReturnable;

	private String status;

	private Double prorataAmount;

	private ConsumerOrderItemMetadata metadata;
}