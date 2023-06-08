package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class CreateTicketBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	@NotEmpty
	private String referenceId;

	@NotNull
	private Integer categoryRootId;

	@NotNull
	private Integer categoryLeafId;

	@NotNull
	private List<String> attachments = new ArrayList<String>();

	@NotNull
	private TicketDetailsBean details = TicketDetailsBean.newInstance();

	public static CreateTicketBean newInstance() {
		return new CreateTicketBean();
	}
}