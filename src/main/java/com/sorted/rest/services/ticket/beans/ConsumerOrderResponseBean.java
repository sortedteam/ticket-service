package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@ApiModel(description = "Order Response Bean extending the List Bean")
@Data
public class ConsumerOrderResponseBean extends ConsumerOrderListBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private Double totalMrpGrossAmount;

	private Double totalSpGrossAmount;

	private Double totalDiscountAmount;

	private Double totalTaxAmount;

	private Double totalExtraFeeAmount;

	private Double refundAmount;

	private ConsumerOrderTaxDetails taxDetails;

	private ConsumerOrderAdditionalDiscount totalAdditionalDiscount;

	private ConsumerOrderExtraFeeDetail extraFeeDetails;

	private String channel;

	private List<ConsumerOrderItemResponseBean> orderItems;

	private List<ConsumerOrderCharges> orderCharges;

	private String notes;

	private OfferData offerData;

	private String checkoutMessage;

	private String etaMessage;

	private String bufferETAMessage;

	private Boolean walletError = false;

	private Double walletBalance;

	private Double walletLoyaltyCoins;

	private Double finalBillCoins;

	private Double coinsReceived;

	private Double cartCoinsEarned;

	private BigDecimal coinsAfterDeduction;

	private Long orderCount;

	private List<String> cartImages;

	private String storeName = "Sorted Store";

	private Integer slotId;

	private String addressError;

	private String walletErrorMessage;

	private Date deliveryDate;

	private ConsumerDeliveryCharges deliveryChargesAndOffer;

	public static ConsumerOrderResponseBean newInstance() {
		return new ConsumerOrderResponseBean();
	}

}
