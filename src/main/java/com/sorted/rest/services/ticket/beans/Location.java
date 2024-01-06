package com.sorted.rest.services.ticket.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@ApiModel(description = "location")
@Data
public class Location implements Serializable {

	private static final long serialVersionUID = 1632130020507453176L;

	@JsonProperty("lat")
	private String latitude;

	@JsonProperty("long")
	private String longitude;

}
