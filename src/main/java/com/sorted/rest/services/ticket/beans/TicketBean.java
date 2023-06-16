package com.sorted.rest.services.ticket.beans;

import com.fasterxml.jackson.annotation.JsonFormat;
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

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "IST")
	private Date assignedAt;

	private List<String> attachments = new ArrayList<String>();

	private String status;

	private String platform;

	private String remarks;

	private TicketDetailsBean details = TicketDetailsBean.newInstance();

	private ResolutionDetailsBean resolutionDetails = ResolutionDetailsBean.newInstance();

	private TicketCategoryNode category;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "IST")
	private Date createdAt;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "IST")
	private Date modifiedAt;

	public static TicketBean newInstance() {
		return new TicketBean();
	}
}