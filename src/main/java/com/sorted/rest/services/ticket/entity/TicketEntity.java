package com.sorted.rest.services.ticket.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.ticket.beans.ResolutionDetailsBean;
import com.sorted.rest.services.ticket.beans.TicketDetailsBean;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = TicketConstants.TICKETS_TABLE_NAME)
@DynamicUpdate
@Data
public class TicketEntity extends BaseEntity implements TicketEntityConstants {

	private static final long serialVersionUID = -7538803140039235801L;

	@Id
	@Column(updatable = false, nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String requesterEntityType;

	@Column(nullable = false)
	private String requesterEntityId;

	@Column
	private String requesterEntityCategory;

	@Column(nullable = false)
	private String referenceId;

	@Column(name = "category_root_id", insertable = false, updatable = false, nullable = false)
	private Integer categoryRootId;

	@Column(name = "category_leaf_id", insertable = false, updatable = false, nullable = false)
	private Integer categoryLeafId;

	@Column(nullable = false)
	private Integer priority;

	@Column(nullable = false)
	private String assignedTeam;

	@Column(nullable = false)
	private Date assignedAt;

	@Column
	private String remarks = "Ticket Created";

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb", nullable = false)
	private List<String> attachments = new ArrayList<String>();

	@Column(nullable = false)
	private String status;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb", nullable = false)
	private TicketDetailsBean details = TicketDetailsBean.newInstance();

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb", nullable = false)
	private ResolutionDetailsBean resolutionDetails = ResolutionDetailsBean.newInstance();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_root_id", referencedColumnName = "id", updatable = false)
	private TicketCategoryEntity categoryRoot;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_leaf_id", referencedColumnName = "id", updatable = false)
	private TicketCategoryEntity categoryLeaf;

	public static TicketEntity newInstance() {
		return new TicketEntity();
	}
}