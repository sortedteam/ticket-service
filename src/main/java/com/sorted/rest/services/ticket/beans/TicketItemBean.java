package com.sorted.rest.services.ticket.beans;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class TicketItemBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	private Long id;

	private Long ticketId;

	private Integer priority;

	private String assignedTeam;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "IST")
	private Date assignedAt;

	private List<String> attachments;

	private String status;

	private String platform;

	private String remarks;

	private ResolutionDetailsBean details;

	private TicketCategoryNode category;

	@JsonIgnore
	private TicketCategoryBean categoryLeaf;

	private List<String> updateActions;

	private String leafCategoryDesc;

	private String leafParentCategoryDesc;

	public static TicketItemBean newInstance() {
		return new TicketItemBean();
	}
}