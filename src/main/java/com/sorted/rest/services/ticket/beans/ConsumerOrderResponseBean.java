package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@ApiModel(description = "Order Response Bean extending the List Bean")
@Data
public class ConsumerOrderResponseBean extends ConsumerOrderListBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = "total mrp gross amount", allowEmptyValue = false)
	@NotNull
	private Double totalMrpGrossAmount;

	@ApiModelProperty(value = "total Sp Gross Amount", allowEmptyValue = false)
	@NotNull
	private Double totalSpGrossAmount;

	@ApiModelProperty(value = "total Discount Amount", allowEmptyValue = false)
	@NotNull
	private Double totalDiscountAmount;

	@ApiModelProperty(value = "total Tax Amount", allowEmptyValue = false)
	@NotNull
	private Double totalTaxAmount;

	@ApiModelProperty(value = "total Extra Fee Amount", allowEmptyValue = false)
	@NotNull
	private Double totalExtraFeeAmount;

	@ApiModelProperty(value = "refund Amount", allowEmptyValue = false)
	@NotNull
	private Double refundAmount;

	@ApiModelProperty(value = "Tax Details", allowEmptyValue = false)
	@NotNull
	private ConsumerOrderTaxDetails taxDetails;

	@ApiModelProperty(value = "total Additional Discount", allowEmptyValue = false)
	@NotNull
	private ConsumerOrderAdditionalDiscount totalAdditionalDiscount;

	@ApiModelProperty(value = "extra Fee Details", allowEmptyValue = false)
	@NotNull
	private ConsumerOrderExtraFeeDetail extraFeeDetails;

	@ApiModelProperty(value = "channel", allowEmptyValue = false)
	@NotNull
	private String channel;

	@ApiModelProperty(value = "Order Items", allowEmptyValue = false)
	@NotNull
	private List<ConsumerOrderItemResponseBean> orderItems;

	@ApiModelProperty(value = "Order Charges", allowEmptyValue = true)
	private List<ConsumerOrderCharges> orderCharges;

	@ApiModelProperty(value = "Order Notes", allowEmptyValue = true)
	private String notes;

	@ApiModelProperty(value = "Offer Data", allowEmptyValue = true)
	private OfferData offerData;

	@ApiModelProperty(value = "Checkout Message", allowEmptyValue = true)
	private String checkoutMessage;

	@ApiModelProperty(value = "ETA Message", allowEmptyValue = true)
	private String etaMessage;

	@ApiModelProperty(value = "Additional Buffer Time", allowEmptyValue = true)
	private String bufferETAMessage;

	@ApiModelProperty(value = "Wallet check", allowEmptyValue = false)
	private Boolean walletError = false;

	@ApiModelProperty(value = "Wallet check", allowEmptyValue = true)
	private Double walletBalance;

	@ApiModelProperty(value = "Wallet Loyalty Coins", allowEmptyValue = true)
	private Double walletLoyaltyCoins;

	@ApiModelProperty(value = "Final Bill Coins", allowEmptyValue = true)
	private Double finalBillCoins;

	@ApiModelProperty(value = "Total coins given to the user based on cart", allowEmptyValue = true)
	private Double coinsReceived;

	@ApiModelProperty(value = "Total coins given to the user based on cart", allowEmptyValue = true)
	private Double cartCoinsEarned;

	@ApiModelProperty(value = "Total coins given to the user based on cart", allowEmptyValue = true)
	private BigDecimal coinsAfterDeduction;

	@ApiModelProperty(value = "Order Count", allowEmptyValue = true)
	private Long orderCount;

	@ApiModelProperty(value = "Cart Images", allowEmptyValue = true)
	private List<String> cartImages;

	@ApiModelProperty(value = "Cart Images", allowEmptyValue = true)
	private String storeName = "Sorted Store";

	@ApiModelProperty(value = "Slot ID", allowEmptyValue = true)
	private Integer slotId;

	@ApiModelProperty(value = "Delivery Date", allowEmptyValue = true)
	private Date deliveryDate;

	public static ConsumerOrderResponseBean newInstance() {
		return new ConsumerOrderResponseBean();
	}

}
