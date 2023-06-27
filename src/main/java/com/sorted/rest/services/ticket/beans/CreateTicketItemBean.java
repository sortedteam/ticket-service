package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class CreateTicketItemBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	@NotNull
	private Integer categoryLeafId;

	@NotNull
	private List<String> attachments = new ArrayList<String>();

	@NotNull
	private TicketDetailsBean details = TicketDetailsBean.newInstance();

	public static CreateTicketItemBean newInstance() {
		return new CreateTicketItemBean();
	}
}