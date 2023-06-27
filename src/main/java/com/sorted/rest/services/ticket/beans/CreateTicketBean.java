package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class CreateTicketBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	private String referenceId;

	@NotNull
	private Integer categoryRootId;

	private List<CreateTicketItemBean> items;

	public static CreateTicketBean newInstance() {
		return new CreateTicketBean();
	}
}