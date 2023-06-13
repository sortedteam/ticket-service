package com.sorted.rest.services.ticket.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class StoreReturnItemData implements Serializable {

	private static final long serialVersionUID = -6385345671728070023L;

	private Long id;

	private Double quantity;

	@JsonProperty("sku_code")
	private String skuCode;

	private String day;

	private String remarks;
}
