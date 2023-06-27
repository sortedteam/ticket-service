package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TicketListViewBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	private Long id;

	private String requesterEntityType;

	private String requesterEntityId;

	private String requesterEntityCategory;

	private String referenceId;

	private Integer categoryRootId;

	private Integer priority;

	private String assignedTeam;

	private Date assignedAt;

	private String status;

	private TicketCategoryNode category;

	private Date createdAt;

	private Date modifiedAt;

	public static TicketListViewBean newInstance() {
		return new TicketListViewBean();
	}

}