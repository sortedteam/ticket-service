package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@ApiModel(description = "Resolution Details Bean")
@Data
public class ResolutionDetailsBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private String description;

	private OrderDetailsBean orderDetails;

	public static ResolutionDetailsBean newInstance() {
		return new ResolutionDetailsBean();
	}
}
