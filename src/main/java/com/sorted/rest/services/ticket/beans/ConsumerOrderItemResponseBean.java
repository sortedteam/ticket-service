package com.sorted.rest.services.ticket.beans;

import com.sorted.rest.common.beans.ErrorBean;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@ApiModel(description = "Order Item Response Bean")
@Data
public class ConsumerOrderItemResponseBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private UUID id;

	private UUID orderId;

	private String skuCode;

	private String productName;

	@ApiModelProperty(value = "Order Item uom", allowEmptyValue = false)
	@NotNull
	private String uom;

	@ApiModelProperty(value = "Order Item sku image url", allowEmptyValue = false)
	@NotNull
	private String imageUrl;

	@ApiModelProperty(value = "Order Item category Id", allowEmptyValue = false)
	@NotNull
	private Integer categoryId;

	@ApiModelProperty(value = "Order Item category name", allowEmptyValue = false)
	@NotNull
	private String categoryName;

	@ApiModelProperty(value = "Order Item ordered quantity", allowEmptyValue = false)
	@NotNull
	private Double orderedQty;

	@ApiModelProperty(value = "Order Item final quantity", allowEmptyValue = false)
	@NotNull
	private Double finalQuantity;

	@ApiModelProperty(value = "Order Item sale price per unit", allowEmptyValue = false)
	@NotNull
	private Double salePrice;

	@ApiModelProperty(value = "Order Item marked price per unit", allowEmptyValue = false)
	@NotNull
	private BigDecimal markedPrice;

	@ApiModelProperty(value = "Order Item mrp gross amount", allowEmptyValue = false)
	@NotNull
	private Double mrpGrossAmount;

	@ApiModelProperty(value = "Order Item sp gross amount", allowEmptyValue = false)
	@NotNull
	private Double spGrossAmount;

	@ApiModelProperty(value = "Order Item discount", allowEmptyValue = true)
	private Double discountAmount;

	@ApiModelProperty(value = "Order Item additonal discounts", allowEmptyValue = false)
	@NotNull
	private ConsumerOrderAdditionalDiscount additionalDiscount;

	@ApiModelProperty(value = "Order Item Tax amount", allowEmptyValue = false)
	@NotNull
	private Double taxAmount;

	@ApiModelProperty(value = "Order Item tax details", allowEmptyValue = false)
	@NotNull
	private ConsumerOrderTaxDetails taxDetails;

	@ApiModelProperty(value = "Order Item refund amount", allowEmptyValue = true)
	private Double refundAmount;

	@ApiModelProperty(value = "Order Item final amount", allowEmptyValue = false)
	@NotNull
	private Double finalAmount;

	@ApiModelProperty(value = "Final Bill Coins for the item", allowEmptyValue = false)
	private Double finalItemBillCoins;

	@ApiModelProperty(value = "Order Item is returnable flag", allowEmptyValue = false)
	private Double itemCoinsEarned;

	@ApiModelProperty(value = "Order Item is returnable flag", allowEmptyValue = false)
	private Integer isCoinsRedeemedItem;

	@ApiModelProperty(value = "Order Item is refundable flag", allowEmptyValue = false)
	@NotNull
	private Integer isRefundable;

	@ApiModelProperty(value = "Order Item is returnable flag", allowEmptyValue = false)
	@NotNull
	private Integer isReturnable;

	@ApiModelProperty(value = "Order Item status", allowEmptyValue = false)
	@NotNull
	private String status;

	@ApiModelProperty(value = "Order Item error message", allowEmptyValue = false)
	@NotNull
	private ErrorBean error;

	@ApiModelProperty(value = "Prorata Amount", allowEmptyValue = false)
	@NotNull
	private Double prorataAmount;
}