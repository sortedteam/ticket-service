package com.sorted.rest.services.ticket.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.ticket.beans.TicketMetadataBean;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = TicketConstants.TICKETS_TABLE_NAME)
@DynamicUpdate
@Data
public class TicketEntity extends BaseEntity {

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

	@Column(unique = true)
	private String referenceId;

	@Column(name = "category_root_id", insertable = false, updatable = false, nullable = false)
	private Integer categoryRootId;

	@Column(nullable = false)
	private Integer isClosed;

	@Column(nullable = false)
	private Integer hasDraft;

	@Column(nullable = false)
	private Integer hasPending;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb", nullable = false)
	private TicketMetadataBean metadata = TicketMetadataBean.newInstance();

	@Column(nullable = false)
	private java.sql.Date lastAddedOn;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_root_id", referencedColumnName = "id", updatable = false)
	private TicketCategoryEntity categoryRoot;

	@Where(clause = "active = 1")
	@OrderBy(clause = "createdAt DESC")
	@OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonManagedReference
	private List<TicketItemEntity> items;

	@Transient
	private Boolean hasNew = false;

	@Transient
	private Boolean hasUpdatedDraft = false;

	public void addTicketItems(List<TicketItemEntity> newItems) {
		if (CollectionUtils.isEmpty(items)) {
			items = new ArrayList<>();
		}
		items.addAll(newItems);
	}

	public static TicketEntity newInstance() {
		return new TicketEntity();
	}
}