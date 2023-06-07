package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TicketHistoryBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	private Long id;

	private Long ticketId;

	private String action;

	private TicketActionDetailsBean details = TicketActionDetailsBean.newInstance();

	private Date createdAt;

	private Date modifiedAt;

	public static TicketHistoryBean newInstance() {
		return new TicketHistoryBean();
	}
}