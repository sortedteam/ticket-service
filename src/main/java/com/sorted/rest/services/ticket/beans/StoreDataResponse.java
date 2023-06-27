package com.sorted.rest.services.ticket.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class StoreDataResponse implements Serializable {

	private static final long serialVersionUID = 493517626137997750L;

	private String id;

	private String name;

	@JsonProperty("store_id")
	private String storeId;

	private String contactNumber;

	private String ownerName;

	private String status;

	private Boolean isActive;

}
