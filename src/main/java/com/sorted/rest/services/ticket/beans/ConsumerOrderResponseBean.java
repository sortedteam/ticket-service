package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@ApiModel(description = "Order Response Bean extending the List Bean")
@Data
public class ConsumerOrderResponseBean extends ConsumerOrderListBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private List<ConsumerOrderItemResponseBean> orderItems;

	private String storeName;

	private Integer slotId;

	private Date deliveryDate;

	public static ConsumerOrderResponseBean newInstance() {
		return new ConsumerOrderResponseBean();
	}

}
