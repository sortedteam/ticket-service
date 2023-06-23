package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class UpdateTicketBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	@NotNull
	private Long id;

	@NotNull
	private Long itemId;

	@NotNull
	private Integer categoryLeafId;

	@NotNull
	private String action;

	private Integer priority;

	private String assignedTeam;

	private List<String> attachments;

	private String remarks;

	private Double resolvedQuantity;

	public static UpdateTicketBean newInstance() {
		return new UpdateTicketBean();
	}

}