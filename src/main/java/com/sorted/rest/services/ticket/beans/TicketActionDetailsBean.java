package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "Resolution Details Bean")
@Data
public class TicketActionDetailsBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private String remarks;

	private List<String> attachments = new ArrayList<>();

	private UserDetail userDetail = UserDetail.newInstance();

	public static TicketActionDetailsBean newInstance() {
		return new TicketActionDetailsBean();
	}
}
