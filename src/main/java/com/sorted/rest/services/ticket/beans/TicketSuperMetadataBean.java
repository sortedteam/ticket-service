package com.sorted.rest.services.ticket.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@ApiModel(description = "Ticket Details Bean")
@Data
@ToString(callSuper = true)
public class TicketSuperMetadataBean extends TicketMetadataBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private StoreSalesDetailsBean storeSalesDetails;

	public static TicketSuperMetadataBean newInstance() {
		return new TicketSuperMetadataBean();
	}
}
