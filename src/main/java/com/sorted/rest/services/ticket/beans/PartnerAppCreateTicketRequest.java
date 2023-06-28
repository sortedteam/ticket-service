package com.sorted.rest.services.ticket.beans;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString(callSuper = true)
public class PartnerAppCreateTicketRequest extends CreateTicketBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	public static PartnerAppCreateTicketRequest newInstance() {
		return new PartnerAppCreateTicketRequest();
	}
}