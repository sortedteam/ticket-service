package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@Data
public class ImsCreateTicketRequest extends CreateTicketBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	@NotEmpty
	private String requesterEntityType;

	@NotEmpty
	private String requesterEntityId;

	public static ImsCreateTicketRequest newInstance() {
		return new ImsCreateTicketRequest();
	}
}