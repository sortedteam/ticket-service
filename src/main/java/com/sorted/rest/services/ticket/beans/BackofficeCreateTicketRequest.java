package com.sorted.rest.services.ticket.beans;

import lombok.Data;
import org.hibernate.type.EntityType;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.Date;

@Data
public class BackofficeCreateTicketRequest extends CreateTicketBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	@NotEmpty
	private EntityType requesterEntityType;

	@NotEmpty
	private String requesterEntityId;

	private Integer priority;

	private String assignedTeam;

	private Date assignedAt;

	public static BackofficeCreateTicketRequest newInstance() {
		return new BackofficeCreateTicketRequest();
	}
}