package com.sorted.rest.services.ticket.beans;

import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@Data
@ToString(callSuper = true)
public class MiddleMileAppCreateTicketRequest extends CreateTicketBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	@NotEmpty
	private String requesterEntityType;

	@NotEmpty
	private String requesterEntityId;

	public static MiddleMileAppCreateTicketRequest newInstance() {
		return new MiddleMileAppCreateTicketRequest();
	}
}