package com.sorted.rest.services.ticket.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class StoreReturnItemData implements Serializable {

	private static final long serialVersionUID = -6385345671728070023L;

	@NotNull
	private Double quantity;

	@JsonProperty("sku_code")
	@NotEmpty
	private String skuCode;

	private String remarks;

	private String qaResult;

	private Double refundQty;
}
