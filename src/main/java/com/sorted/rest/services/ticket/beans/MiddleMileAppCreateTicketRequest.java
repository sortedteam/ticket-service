package com.sorted.rest.services.ticket.beans;

import lombok.Data;
import org.hibernate.type.EntityType;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@Data
public class MiddleMileAppCreateTicketRequest extends CreateTicketBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	@NotEmpty
	private EntityType requesterEntityType;

	@NotEmpty
	private String requesterEntityId;

	public static MiddleMileAppCreateTicketRequest newInstance() {
		return new MiddleMileAppCreateTicketRequest();
	}
}