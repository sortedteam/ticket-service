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
@Table(name = TicketConstants.TICKET_ITEMS_TABLE_NAME)
@DynamicUpdate
@Data
public class TicketItemEntity extends BaseEntity implements TicketEntityConstants {

	private static final long serialVersionUID = -7538803140039235801L;

	@Id
	@Column(updatable = false, nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "ticket_id", insertable = false, updatable = false, nullable = false)
	private Long ticketId;

	@Column(name = "category_leaf_id", insertable = false, updatable = false, nullable = false)
	private Integer categoryLeafId;

	@Column(nullable = false)
	private Integer priority;

	@Column(nullable = false)
	private String assignedTeam;

	@Column(nullable = false)
	private Date assignedAt;

	@Column(nullable = false)
	private String platform;

	@Column
	private String remarks;

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
	@JoinColumn(name = "category_leaf_id", referencedColumnName = "id", updatable = false)
	private TicketCategoryEntity categoryLeaf;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ticket_id", referencedColumnName = "id", nullable = false)
	private TicketEntity ticket;

	@Transient
	private Boolean isNew = false;

	@Transient
	private Boolean wasDraft = false;

	public static TicketItemEntity newInstance() {
		return new TicketItemEntity();
	}
}