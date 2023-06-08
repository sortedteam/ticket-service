package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class PartnerAppCreateTicketRequest extends CreateTicketBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	public static PartnerAppCreateTicketRequest newInstance() {
		return new PartnerAppCreateTicketRequest();
	}
}