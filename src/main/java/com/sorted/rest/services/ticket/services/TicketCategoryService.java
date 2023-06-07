package com.sorted.rest.services.ticket.services;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.ticket.beans.TicketCategoryNode;
import com.sorted.rest.services.ticket.entity.TicketCategoryEntity;
import com.sorted.rest.services.ticket.repository.TicketCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mohit on 20.8.22.
 */
@Service
public class TicketCategoryService implements BaseService<TicketCategoryEntity> {

	@Autowired
	private TicketCategoryRepository ticketCategoryRepository;

	public List<TicketCategoryNode> getVisibleTicketCategoryNodes() {
		return getTicketCategoryNodeList(getVisibleTicketCategories());
	}

	private List<TicketCategoryEntity> getVisibleTicketCategories() {
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
				parentTicketCategoryNode.addChild(currentTicketCategoryNode);
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

	public TicketCategoryNode getTicketCategoryNodeById(Integer id) {
		return getTicketCategoryNode(getVisibleTicketCategories(), id);
	}

	private TicketCategoryNode getTicketCategoryNode(List<TicketCategoryEntity> categories, Integer id) {
		Map<Integer, TicketCategoryNode> categoryMap = getTicketCategoriesMap(categories);
		for (TicketCategoryNode ticketCategoryNode : categoryMap.values()) {
			if (ticketCategoryNode.getId() == id) {
				return ticketCategoryNode;
			}
		}
		return null;
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