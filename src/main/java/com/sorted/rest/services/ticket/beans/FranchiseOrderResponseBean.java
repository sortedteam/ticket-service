package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@ApiModel(description = "Franchise Order Response Bean extending the List Bean")
@Data
public class FranchiseOrderResponseBean extends FranchiseOrderListBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = "total mrp gross amount", allowEmptyValue = false)
	@NotNull
	private Double totalMrpGrossAmount;

	@ApiModelProperty(value = "total Sp Gross Amount", allowEmptyValue = false)
	@NotNull
	private Double totalSpGrossAmount;

	@ApiModelProperty(value = "total item count", allowEmptyValue = false)
	@NotNull
	private Integer itemCount;

	@ApiModelProperty(value = "total final bill amount", allowEmptyValue = false)
	@NotNull
	private Double finalBillAmount;

	@ApiModelProperty(value = "total discount amount", allowEmptyValue = false)
	@NotNull
	private Double totalDiscountAmount;

	@ApiModelProperty(value = "total tax amount", allowEmptyValue = false)
	@NotNull
	private Double totalTaxAmount;

	@ApiModelProperty(value = "total extra fee amount", allowEmptyValue = false)
	@NotNull
	private Double totalExtraFeeAmount;

	@ApiModelProperty(value = "refund amount ", allowEmptyValue = false)
	@NotNull
	private Double refundAmount;

	@ApiModelProperty(value = "amount received ", allowEmptyValue = false)
	@NotNull
	private Double amountReceived;

	@ApiModelProperty(value = "Order Items", allowEmptyValue = false)
	@NotNull
	private List<FranchiseOrderItemResponseBean> orderItems;

	@ApiModelProperty(value = "Order Charges", allowEmptyValue = true)
	private List<OrderCharges> orderCharges;

	@ApiModelProperty(value = "estimated amount", allowEmptyValue = true)
	private Double estimatedBillAmount;

	@ApiModelProperty(value = "Is srp store", allowEmptyValue = false)
	private Integer isSrpStore;

	@ApiModelProperty(value = "category based Count", allowEmptyValue = true)
	private Map<String, Long> categoryCount;

	@ApiModelProperty(value = "Adjustment Details", allowEmptyValue = true)
	private List<WalletStatementBean> adjustments;

	@ApiModelProperty(value = "Total Adjustment", allowEmptyValue = true)
	private Double totalAdjustment;

	@ApiModelProperty(value = "Total Bill After Adjustment", allowEmptyValue = true)
	private Double totalBillAfterAdjustment;

	@ApiModelProperty(value = "valid Min Order Rule", allowEmptyValue = true)
	private Boolean validMinOrderRule;

	@ApiModelProperty(value = "Min Amount order Msg Home", allowEmptyValue = true)
	private String minAmountMsgHome;

	@ApiModelProperty(value = "Min Amount order Msg cart", allowEmptyValue = true)
	private String minAmountMsgCart;

	@ApiModelProperty(value = "Order Count", allowEmptyValue = true)
	private Long orderCount;

	@ApiModelProperty(value = "Offer Data", allowEmptyValue = true)
	private OfferData offerData;

	public static FranchiseOrderResponseBean newInstance() {
		return new FranchiseOrderResponseBean();
	}
}
