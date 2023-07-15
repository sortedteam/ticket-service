package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class TicketCategoryViewBean implements Serializable, Comparable<TicketCategoryViewBean> {

	private static final long serialVersionUID = -7538803140039235801L;

	private Integer id;

	private String label;

	private String description;

	public static TicketCategoryViewBean newInstance() {
		return new TicketCategoryViewBean();
	}

	@Override
	public int compareTo(TicketCategoryViewBean o) {
		return Integer.compare(o.id, this.id);
	}
}