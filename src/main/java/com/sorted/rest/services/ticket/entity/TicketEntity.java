package com.sorted.rest.services.ticket.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.sorted.rest.services.ticket.beans.ResolutionDetailsBean;
import com.sorted.rest.services.ticket.beans.TicketDetailsBean;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = TicketConstants.TICKETS_TABLE_NAME)
@DynamicUpdate
@Data
public class TicketEntity implements TicketEntityConstants {

	private static final long serialVersionUID = -7538803140039235801L;

	@Id
	@Column(updatable = false, nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column
	private String requesterEntityType;

	@Column
	private String requesterEntityId;

	@Column
	private String requesterEntityCategory;

	@Column
	private String referenceId;

	@Column(name = "category_root_id", insertable = false, updatable = false, nullable = false)
	private Integer categoryRootId;

	@Column(name = "category_root_id", insertable = false, updatable = false, nullable = false)
	private Integer categoryLeafId;

	@Column
	private Integer priority;

	@Column
	private String assignedTeam;

	@Column
	private Date assignedAt;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb", nullable = false)
	private List<String> attachments = new ArrayList<String>();

	@Column
	private String status;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb", nullable = false)
	private TicketDetailsBean details = TicketDetailsBean.newInstance();

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb", nullable = false)
	private ResolutionDetailsBean resolutionDetails = ResolutionDetailsBean.newInstance();

	@Column(name = "created_at", nullable = false, updatable = false)
	@CreatedDate
	private Date createdAt;

	@Column(name = "modified_at", nullable = false)
	@LastModifiedDate
	private Date modifiedAt;

	@Column(name = "created_by", nullable = false, updatable = false)
	@CreatedBy
	private String createdBy;

	@Column(name = "modified_by", nullable = false)
	@LastModifiedBy
	private String modifiedBy;

	@Column(name = "active", nullable = false)
	private Integer active = 1;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_root_id", referencedColumnName = "id", updatable = false)
	private TicketCategoryEntity categoryRoot;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_leaf_id", referencedColumnName = "id", updatable = false)
	private TicketCategoryEntity categoryLeaf;

	@Where(clause = "active = 1")
	@org.hibernate.annotations.OrderBy(clause = "created_at ASC")
	@OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonManagedReference
	private List<TicketHistoryEntity> history;
}