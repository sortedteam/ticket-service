package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@ApiModel(description = "Order Details Request Bean")
@Data
public class StoreReturnTicketRequest implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private String storeId;

	private String orderId;

	private List<StoreReturnItemData> storeReturnItemDataList;

	public static StoreReturnTicketRequest newInstance() {
		return new StoreReturnTicketRequest();
	}
}
