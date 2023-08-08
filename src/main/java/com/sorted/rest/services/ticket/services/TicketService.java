package com.sorted.rest.services.ticket.services;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.constants.Operation;
import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.dbsupport.pagination.FilterCriteria;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.common.upload.UploadService;
import com.sorted.rest.services.ticket.beans.TicketCategoryNode;
import com.sorted.rest.services.ticket.clients.TicketClientService;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketCategoryRoot;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketStatus;
import com.sorted.rest.services.ticket.entity.TicketCategoryEntity;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.repository.TicketRepository;
import com.sorted.rest.services.ticket.utils.TicketActionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class TicketService implements BaseService<TicketEntity> {

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private UploadService uploadService;

	@Autowired
	private TicketActionUtils ticketActionUtils;

	@Autowired
	private TicketCategoryService ticketCategoryService;

	@Autowired
	private TicketClientService ticketClientService;

	AppLogger _LOGGER = LoggingManager.getLogger(TicketService.class);

	public TicketEntity findById(Long id) {
		Optional<TicketEntity> resultOpt = ticketRepository.findById(id);
		if (resultOpt.isPresent()) {
			return resultOpt.get();
		}
		return null;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public TicketEntity saveNewParentTicket(TicketEntity entity) {
		entity.setClosedCount(0);
		entity.setDraftCount(0);
		entity.setPendingCount(0);
		entity.setCancelledCount(0);
		entity = ticketRepository.save(entity);
		return entity;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public TicketEntity saveTicketWithItems(TicketEntity entity, List<TicketItemEntity> items) {
		entity.addTicketItems(items);
		for (TicketItemEntity item : items) {
			item.setTicket(entity);
		}
		return saveTicketWithUpdatedItems(entity);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public TicketEntity saveTicketWithUpdatedItems(TicketEntity entity) {
		Boolean hasNew = entity.getHasNew();
		Integer draftCount = 0, draftCountOld = entity.getDraftCount();
		Integer pendingCount = 0, pendingCountOld = entity.getPendingCount();
		Integer closedCount = 0, closedCountOld = entity.getClosedCount();
		Integer cancelledCount = 0, cancelledCountOld = entity.getCancelledCount();
		for (TicketItemEntity item : entity.getItems()) {
			if (item.getStatus().equals(TicketStatus.DRAFT)) {
				draftCount++;
			} else if (item.getStatus().equals(TicketStatus.IN_PROGRESS)) {
				pendingCount++;
			} else if (item.getStatus().equals(TicketStatus.CLOSED)) {
				closedCount++;
			} else if (item.getStatus().equals(TicketStatus.CANCELLED)) {
				cancelledCount++;
			}
		}
		entity.setClosedCount(closedCount);
		entity.setDraftCount(draftCount);
		entity.setPendingCount(pendingCount);
		entity.setCancelledCount(cancelledCount);
		entity.setModifiedAt(new Date());
		entity = ticketRepository.save(entity);
		ticketActionUtils.addParentTicketHistory(entity, hasNew, draftCountOld, pendingCountOld, closedCountOld, cancelledCountOld);
		return entity;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<TicketEntity> saveAll(List<TicketEntity> entities) {
		List<TicketEntity> result = StreamSupport.stream(ticketRepository.saveAll(entities).spliterator(), false).collect(Collectors.toList());
		return result;
	}

	public List<String> uploadFiles(MultipartFile[] files) {
		List<String> uploadedFiles = new ArrayList<>();
		String bucketName = ParamsUtils.getParam("SORTED_FILES_BUCKET_NAME");
		String subDirectory = ParamsUtils.getParam("TICKET_FILES_SUBDIRECTORY");
		try {
			for (MultipartFile file : files) {
				String fileExt = uploadService.findFileExt(file.getOriginalFilename());
				String fileName = file.getOriginalFilename();
				int index = fileName.indexOf(fileExt);
				fileName = String.format("%s-%s.%s", fileName.substring(0, index - 1), Instant.now().toEpochMilli(), fileExt);
				_LOGGER.info(String.format("Uploading attachment - %s to s3", fileName));
				Object response = uploadService.uploadFile(bucketName, subDirectory, file.getBytes(), fileName);
				uploadedFiles.add(ParamsUtils.getParam("CLOUDFRONT_URL").concat("/").concat(response.toString()));
			}
			return uploadedFiles;
		} catch (IOException err) {
			_LOGGER.error("Files Uploading ", err);
			throw new ValidationException(new ErrorBean(Errors.UPDATE_FAILED, "Error while uploading attachments", "attachments"));
		}
	}

	public List<TicketEntity> findByReferenceIdAndCategoryRootId(String referenceId, Integer categoryRootId) {
		List<TicketEntity> tickets = new ArrayList<>();
		if (referenceId != null) {
			Map<String, Object> filters = new HashMap();
			filters.put("referenceId", referenceId);
			filters.put("categoryRootId", categoryRootId);
			tickets = findAllRecords(filters);
		}
		if (tickets.isEmpty()) {
			tickets.add(null);
		}
		return tickets;
	}

	public PageAndSortResult<TicketEntity> getAllTicketsPaginated(Integer pageSize, Integer pageNo, Map<String, Object> filters,
			Map<String, PageAndSortRequest.SortDirection> sort) {
		PageAndSortResult<TicketEntity> tickets = null;
		try {
			tickets = findPagedRecords(filters, sort, pageSize, pageNo);
		} catch (Exception e) {
			_LOGGER.error(e);
			throw new ValidationException(ErrorBean.withError("FETCH_ERROR", e.getMessage(), null));
		}
		return tickets;
	}

	public Map<String, Object> getFilters(String requesterEntityId, List<String> requesterEntityIds, String requesterEntityCategory, Boolean orderRelated,
			String lastAddedOn, Boolean hasDraft, Boolean hasPending, Boolean hasClosed, List<TicketCategoryEntity> ticketCategoryEntities)
			throws ParseException {
		Map<String, Object> filters = new HashMap<>();
		if (!requesterEntityIds.isEmpty()) {
			filters.put("requesterEntityId", requesterEntityIds);
		}
		if (requesterEntityCategory != null) {
			filters.put("requesterEntityCategory", requesterEntityCategory);
		}
		List<Integer> categoryRootIds = getCategoryRoots(ticketCategoryEntities, orderRelated);
		filters.remove("orderRelated");
		filters.put("categoryRootId", categoryRootIds);

		if (requesterEntityId == null) {
			Pair<Date, Date> dates = getFilterDates(lastAddedOn);
			if (filters.containsKey("lastAddedOn")) {
				filters.remove("lastAddedOn");
			}
			filters.put("fromDate", new FilterCriteria("lastAddedAt", dates.getLeft(), Operation.GTE));
			filters.put("toDate", new FilterCriteria("lastAddedAt", dates.getRight(), Operation.LTE));
		}

		if (hasDraft != null) {
			filters.put("hasDraft", new FilterCriteria("draftCount", 0, hasDraft ? Operation.GT : Operation.EQUALS));
		}

		if (hasPending != null) {
			filters.put("hasPending", new FilterCriteria("pendingCount", 0, hasPending ? Operation.GT : Operation.EQUALS));
		}

		if (hasClosed != null) {
			filters.put("hasClosed", new FilterCriteria("closedCount", 0, hasClosed ? Operation.GT : Operation.EQUALS));
		}
		return filters;
	}

	private List<Integer> getCategoryRoots(List<TicketCategoryEntity> ticketCategoryEntities, Boolean orderRelated) {
		TicketCategoryNode categoryNode = ticketCategoryService.getTicketCategoryNodeByLabel(ticketCategoryEntities,
				orderRelated ? TicketCategoryRoot.ORDER_ISSUE.toString() : TicketCategoryRoot.OTHER_ISSUES.toString());
		List<Integer> categoryRootIds = new ArrayList<>();
		if (orderRelated) {
			categoryRootIds.add(categoryNode.getId());
		} else {
			for (TicketCategoryNode child : categoryNode.getChildren()) {
				categoryRootIds.add(child.getId());
			}
		}
		if (categoryRootIds.isEmpty()) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, "Any relevant issue ticket category root not found", null));
		}
		return categoryRootIds;
	}

	private Pair<Date, Date> getFilterDates(String lastAddedOn) throws ParseException {
		Date fromDate, toDate;
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		formatter.setTimeZone(TimeZone.getDefault());
		if (lastAddedOn != null) {
			fromDate = DateUtils.addMinutes(formatter.parse(lastAddedOn), -330);
			toDate = DateUtils.addDays(fromDate, 1);
		} else {
			toDate = new Date();
			fromDate = DateUtils.addDays(toDate, -1);
		}
		return Pair.of(fromDate, toDate);
	}

	public List<TicketEntity> getCategoryLeafFilteredTickets(String requesterEntityId, List<String> requesterEntityIds, String requesterEntityCategory,
			Boolean orderRelated, String lastAddedOn, Boolean hasDraft, Boolean hasPending, Boolean hasClosed,
			List<TicketCategoryEntity> ticketCategoryEntities, Integer categoryLeafParent) throws ParseException {
		Pair<Date, Date> lastAddedDates = getFilterDates(lastAddedOn);
		List<Integer> categoryRootsIn = getCategoryRoots(ticketCategoryEntities, orderRelated);
		Boolean skipCheckRequesterEntity = requesterEntityIds.isEmpty();
		Boolean skipCheckDates = requesterEntityId != null;
		requesterEntityIds.add("dummy_string");
		return ticketRepository.findCustomWithCategoryLeafFilter(skipCheckRequesterEntity, skipCheckDates, requesterEntityIds, requesterEntityCategory,
				lastAddedDates.getLeft(), lastAddedDates.getRight(), hasDraft, hasPending, hasClosed, categoryRootsIn, categoryLeafParent);
	}

	public List<TicketEntity> getOrderRelatedFilteredTickets(Date createdFrom, Date createdTo, Integer categoryRootId, String storeId, String skuCode) {
		if (skuCode == null) {
			skuCode = "dummy_string";
		}
		return ticketRepository.findCustomOrderRelated(createdFrom, createdTo, List.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED), categoryRootId, storeId,
				skuCode);
	}

	@Override
	public Class<TicketEntity> getEntity() {
		return TicketEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return ticketRepository;
	}
}