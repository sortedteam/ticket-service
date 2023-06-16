package com.sorted.rest.services.ticket.beans;

import com.sorted.rest.services.ticket.constants.TicketConstants.FranchiseOrderItemStatus;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@ApiModel(description = "Franchise Order Item Response Bean")
@Data
public class FranchiseOrderItemResponseBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private UUID id;

	private UUID orderId;

	private String skuCode;

	private Double moq;

	private Integer whId;

	private String productName;

	private String imageUrl;

	private Double orderedQty;

	private Double finalQuantity;

	private Double salePrice;

	private BigDecimal markedPrice;

	private Double mrpGrossAmount = 0.0;

	private Double spGrossAmount = 0.0;

	private Double finalAmount = 0.0;

	private FranchiseOrderItemStatus status;

	private Integer orderedCrateQty;

	private Integer finalCrateQty;

	private String uom;

	private Double prorataAmount;

	private FranchiseRefundDetails refundDetails;
}
