package com.sorted.rest.services.ticket.beans;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sorted.rest.services.ticket.constants.TicketConstants.FranchiseOrderStatus;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@ApiModel(description = "Franchise Order Response Bean extending the List Bean")
@Data
public class FranchiseOrderResponseBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private UUID id;

	private String displayOrderId;

	private String storeId;

	private FranchiseOrderStatus status;

	private Double finalBillAmount;

	private String challanUrl;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "IST")
	private Date submittedAt;

	private Date deliveryDate;

	private String slot;

	private Double totalMrpGrossAmount;

	private Double totalSpGrossAmount;

	private Double totalDiscountAmount;

	private Double refundAmount;

	private Double amountReceived;

	private List<FranchiseOrderItemResponseBean> orderItems = new ArrayList<>();

	private Double estimatedBillAmount;

	private Double totalAdjustment;

	private Double totalBillAfterAdjustment;

	public static FranchiseOrderResponseBean newInstance() {
		return new FranchiseOrderResponseBean();
	}
}
