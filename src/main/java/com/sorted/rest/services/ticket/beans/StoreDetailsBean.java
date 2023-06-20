package com.sorted.rest.services.ticket.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class StoreDetailsBean implements Serializable {

	private static final long serialVersionUID = 493517626137997750L;

	private String name;

	@JsonProperty("store_id")
	private String storeId;

	private String contactNumber;

	private String ownerName;

	private String status;

	private Boolean isActive;

	public static StoreDetailsBean newInstance() {
		return new StoreDetailsBean();
	}
}
