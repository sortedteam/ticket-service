package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class TicketBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	private Long id;

	private String requesterEntityType;

	private String requesterEntityId;

	private String requesterEntityCategory;

	private String referenceId;

	private Integer categoryRootId;

	private Integer categoryLeafId;

	private Integer priority;

	private String assignedTeam;

	private Date assignedAt;

	private List<String> attachments = new ArrayList<String>();

	private String status;

	private TicketDetailsBean details = TicketDetailsBean.newInstance();

	private ResolutionDetailsBean resolutionDetails = ResolutionDetailsBean.newInstance();

	private List<TicketHistoryBean> history = new ArrayList<TicketHistoryBean>();

	private TicketCategoryNode category;

	private Date createdAt;

	private Date modifiedAt;
}