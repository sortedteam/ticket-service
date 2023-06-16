package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class UpdateTicketBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	@NotNull
	private Long id;

	private String referenceId;

	@NotNull
	private Integer categoryLeafId;

	private Integer priority;

	private String assignedTeam;

	private Date assignedAt;

	@NotNull
	private List<String> attachments = new ArrayList<String>();

	@NotNull
	private TicketDetailsBean details = TicketDetailsBean.newInstance();

	@NotNull
	private ResolutionDetailsBean resolutionDetails = ResolutionDetailsBean.newInstance();

	public static UpdateTicketBean newInstance() {
		return new UpdateTicketBean();
	}

}