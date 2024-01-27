package com.sorted.rest.services.ticket.beans;

import com.sorted.rest.common.websupport.base.BaseBean;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@ApiModel(description = "Order Response Bean")
@Data
public class ConsumerOrderListBean extends BaseBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private UUID id;

	private UUID customerId;

	private String displayOrderId;

	private String storeId;

	private Double finalBillAmount;

	private Double estimatedBillAmount;

	private String status;

	private Long deliveryAddress; // only address not id

	private ConsumerOrderMetadata metadata;

	public static ConsumerOrderListBean newInstance() {
		return new ConsumerOrderListBean();
	}

}
