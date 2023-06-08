package com.sorted.rest.services.ticket.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

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

	public Integer getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public String getDescription() {
		return description;
	}

	public TicketCategoryNode getParent() {
		return parent;
	}

	public void setParent(TicketCategoryNode parent) {
		this.parent = parent;
	}

	public Integer getParentId() {
		return parentId;
	}

	public void setParentId(Integer id) {
		this.parentId = id;
	}

	public List<TicketCategoryNode> getChildren() {
		return children;
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
