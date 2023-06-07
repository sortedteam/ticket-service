package com.sorted.rest.services.ticket.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = TicketConstants.TICKET_HISTORY_TABLE_NAME)
@DynamicUpdate
@Data
public class TicketHistoryEntity {

	private static final long serialVersionUID = -7538803140039235801L;

	@Id
	@Column(updatable = false, nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "ticket_id", updatable = false, insertable = false, nullable = false)
	private Long ticketId;

	@Column(nullable = false)
	private String action;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb", nullable = false)
	private TicketActionDetailsBean details = TicketActionDetailsBean.newInstance();

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
	@JoinColumn(name = "ticket_id", nullable = false)
	@JsonBackReference
	private TicketEntity ticket;

	public static TicketHistoryEntity newInstance() {
		return new TicketHistoryEntity();
	}
}