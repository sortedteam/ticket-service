package com.sorted.rest.services.ticket.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class UserStoreMappingResponse {

	@JsonProperty("created_at")
	private Date createdAt;

	@JsonProperty("updated_at")
	private Date updatedAt;

	@JsonProperty("updated_by")
	private String updatedBy;

	@JsonProperty("id")
	private String id;

	@JsonProperty("user_id")
	private String userId;

	@JsonProperty("store_id")
	private String storeId;

	@JsonProperty("is_active")
	private Boolean isActive;

}
