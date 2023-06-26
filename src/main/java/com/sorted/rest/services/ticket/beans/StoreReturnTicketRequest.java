package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@ApiModel(description = "Order Details Request Bean")
@Data
public class StoreReturnTicketRequest implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@NotEmpty
	private String storeId;

	@NotEmpty
	private String orderId;

	@Valid
	private List<StoreReturnItemData> storeReturnItemDataList;

	public static StoreReturnTicketRequest newInstance() {
		return new StoreReturnTicketRequest();
	}
}
