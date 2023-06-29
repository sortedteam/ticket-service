package com.sorted.rest.services.ticket.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
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

	private TicketMetadataBean metadata;

	@JsonIgnore
	private TicketCategoryBean categoryRoot;

	private Date lastAddedAt;

	private Integer draftCount;

	private Integer pendingCount;

	private Integer closedCount;

	private Integer cancelledCount;

	private List<TicketItemBean> items;

	public static TicketBean newInstance() {
		return new TicketBean();
	}
}