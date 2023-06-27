package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateTicketFromDraftBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	@NotNull
	private Long id;

	@NotNull
	private Long itemId;

	@NotNull
	private Integer categoryLeafId;

	@NotNull
	private List<String> attachments = new ArrayList<>();

	private String description;

	public static UpdateTicketFromDraftBean newInstance() {
		return new UpdateTicketFromDraftBean();
	}
}