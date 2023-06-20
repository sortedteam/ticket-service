package com.sorted.rest.services.ticket.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.ticket.beans.TicketActionDetailsBean;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

import javax.persistence.*;

@Entity
@Table(name = TicketConstants.TICKET_HISTORY_TABLE_NAME)
@DynamicUpdate
@Data
public class TicketHistoryEntity extends BaseEntity {

	private static final long serialVersionUID = -7538803140039235801L;

	@Id
	@Column(updatable = false, nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "ticket_id", nullable = false)
	private Long ticketId;

	@Column(name = "ticket_item_id", nullable = false)
	private Long ticketItemId;

	@Column(nullable = false)
	private String action;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb", nullable = false)
	private TicketActionDetailsBean details = TicketActionDetailsBean.newInstance();

	public static TicketHistoryEntity newInstance() {
		return new TicketHistoryEntity();
	}
}