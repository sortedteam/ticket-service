package com.sorted.rest.services.ticket.entity;

import com.sorted.rest.services.ticket.constants.TicketConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = TicketConstants.TICKET_CATEGORY_TABLE_NAME)
@DynamicUpdate
@Data
public class TicketCategoryEntity {

	private static final long serialVersionUID = -7538803140039235801L;

	@Id
	@Column(updatable = false, nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column
	private String label;

	@Column
	private String description;

	@Column
	private Integer appVisible;

	@Column
	private Integer isTerminal;

	@Column
	private Integer parentId;

	@Column
	private Integer priority;

	@Column(name = "created_at", nullable = false, updatable = false)
	@CreatedDate
	private Date createdAt;

	@Column(name = "modified_at", nullable = false)
	@LastModifiedDate
	private Date modifiedAt;

	@Column(name = "created_by", nullable = false, updatable = false)
	@CreatedBy
	private String createdBy;

	@Column(name = "modified_by", nullable = false, updatable = true)
	@LastModifiedBy
	private String modifiedBy;

	@Column(name = "active", nullable = false)
	private Integer active = 1;

	@OneToMany(mappedBy = "categoryRoot", fetch = FetchType.LAZY)
	private List<TicketEntity> ticketRoots;

	@OneToMany(mappedBy = "categoryLeaf", fetch = FetchType.LAZY)
	private List<TicketEntity> ticketLeafs;
}