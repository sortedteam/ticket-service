package com.sorted.rest.services.ticket.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = TicketConstants.TICKET_CATEGORY_TABLE_NAME)
@DynamicUpdate
@Data
public class TicketCategoryEntity extends BaseEntity {

	private static final long serialVersionUID = -7538803140039235801L;

	@Id
	@Column(updatable = false, nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(unique = true, nullable = false)
	private String label;

	@Column(nullable = false)
	private String description;

	@Column(nullable = false)
	private Integer appVisible = 1;

	@Column(nullable = false)
	private Integer isTerminal = 0;

	@Column
	private Integer parentId;

	@Column(nullable = false)
	private Integer priority = 1;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb", nullable = false)
	private List<String> onCreateActions = new ArrayList<>();

	@OneToMany(mappedBy = "categoryLeaf", fetch = FetchType.LAZY)
	private List<TicketEntity> ticketLeafs;

	public static TicketCategoryEntity newInstance() {
		return new TicketCategoryEntity();
	}

}