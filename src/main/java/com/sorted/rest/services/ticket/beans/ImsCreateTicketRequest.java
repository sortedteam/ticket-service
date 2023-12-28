package com.sorted.rest.services.ticket.beans;

import com.sorted.rest.services.ticket.constants.TicketConstants.EntityType;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ToString(callSuper = true)
public class ImsCreateTicketRequest extends CreateTicketBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	@NotNull
	private EntityType requesterEntityType;

	@NotEmpty
	private String requesterEntityId;

	public static ImsCreateTicketRequest newInstance() {
		return new ImsCreateTicketRequest();
	}
}