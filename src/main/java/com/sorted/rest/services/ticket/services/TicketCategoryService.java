package com.sorted.rest.services.ticket.services;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.ticket.beans.TicketCategoryNode;
import com.sorted.rest.services.ticket.entity.TicketCategoryEntity;
import com.sorted.rest.services.ticket.repository.TicketCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TicketCategoryService implements BaseService<TicketCategoryEntity> {

	@Autowired
	private TicketCategoryRepository ticketCategoryRepository;

	public List<TicketCategoryNode> getVisibleTicketCategoryNodes() {
		return getTicketCategoryNodeList(getVisibleTicketCategories());
	}

	public TicketCategoryNode getTicketCategoryNodeByLabel(String label) {
		return getTicketCategoryNodeLabel(getVisibleTicketCategories(), label);
	}

	public List<TicketCategoryEntity> getVisibleTicketCategories() {
		Map<String, Object> filters = new HashMap<>();
		filters.put("appVisible", 1);
		return findAllRecords(filters);
	}

	private Map<Integer, TicketCategoryNode> getTicketCategoriesMap(List<TicketCategoryEntity> categories) {
		Map<Integer, TicketCategoryNode> categoryMap = new HashMap<>();
		for (TicketCategoryEntity category : categories) {
			TicketCategoryNode TicketCategoryNode = new TicketCategoryNode(category.getId(), category.getLabel(), category.getDescription());
			categoryMap.put(category.getId(), TicketCategoryNode);
		}

		for (TicketCategoryEntity category : categories) {
			Integer parentId = category.getParentId();
			TicketCategoryNode currentTicketCategoryNode = categoryMap.get(category.getId());
			if (parentId != null) {
				TicketCategoryNode parentTicketCategoryNode = categoryMap.get(parentId);
				if (parentTicketCategoryNode != null) {
					parentTicketCategoryNode.addChild(currentTicketCategoryNode);
				}
			}
		}
		return categoryMap;
	}

	private List<TicketCategoryNode> getTicketCategoryNodeList(List<TicketCategoryEntity> categories) {
		Map<Integer, TicketCategoryNode> categoryMap = getTicketCategoriesMap(categories);
		List<TicketCategoryNode> rootTicketCategoryNodes = new ArrayList<>();
		for (TicketCategoryNode ticketCategoryNode : categoryMap.values()) {
			if (ticketCategoryNode.getParent() == null) {
				rootTicketCategoryNodes.add(ticketCategoryNode);
			}
		}
		return rootTicketCategoryNodes;
	}

	private TicketCategoryNode getTicketCategoryNodeLabel(List<TicketCategoryEntity> categories, String label) {
		Map<Integer, TicketCategoryNode> categoryMap = getTicketCategoriesMap(categories);
		for (TicketCategoryNode ticketCategoryNode : categoryMap.values()) {
			if (ticketCategoryNode.getLabel().equals(label)) {
				return ticketCategoryNode;
			}
		}
		return null;
	}

	public TicketCategoryNode getRootToLeafPathUsingCategoryList(List<TicketCategoryEntity> ticketCategoryEntities, Integer rootId, Integer leafId) {
		Map<Integer, TicketCategoryNode> categoryMap = getTicketCategoriesMap(ticketCategoryEntities);
		TicketCategoryNode leafNode = categoryMap.get(leafId);
		leafNode.resetChildren(Collections.emptyList());
		if (rootId == leafId) {
			return leafNode;
		}
		TicketCategoryNode rootNode = leafNode.getParent();
		while (leafNode.getId() != rootId && leafNode != null) {
			rootNode.resetChildren(Collections.singletonList(leafNode));
			leafNode = rootNode;
			rootNode = leafNode.getParent();
		}
		return leafNode;
	}

	@Override
	public Class<TicketCategoryEntity> getEntity() {
		return TicketCategoryEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return ticketCategoryRepository;
	}

}