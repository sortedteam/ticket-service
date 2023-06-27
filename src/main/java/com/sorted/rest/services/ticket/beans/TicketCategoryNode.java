package com.sorted.rest.services.ticket.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TicketCategoryNode {

	private Integer id;

	private String label;

	private String description;

	private Integer parentId;

	@JsonIgnore
	private TicketCategoryNode parent;

	private List<TicketCategoryNode> children;

	public TicketCategoryNode(int id, String label, String description) {
		this.id = id;
		this.label = label;
		this.description = description;
		this.parent = null;
		this.parentId = null;
		this.children = new ArrayList<>();
	}

	public void addChild(TicketCategoryNode child) {
		child.setParent(this);
		child.setParentId(this.id);
		children.add(child);
	}

	public void resetChildren(List<TicketCategoryNode> children) {
		this.children = children;
	}
}
