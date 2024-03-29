package com.sorted.rest.services.ticket.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sorted.rest.services.ticket.constants.TicketConstants.EntityType;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TicketListViewBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	private Long id;

	private EntityType requesterEntityType;

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

	public static TicketListViewBean newInstance() {
		return new TicketListViewBean();
	}
}