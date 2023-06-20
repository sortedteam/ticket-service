package com.sorted.rest.services.ticket.services;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.common.upload.UploadService;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketStatus;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class TicketService implements BaseService<TicketEntity> {

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private UploadService uploadService;

	AppLogger _LOGGER = LoggingManager.getLogger(TicketService.class);

	public TicketEntity findById(Long id) {
		Optional<TicketEntity> resultOpt = ticketRepository.findById(id);
		if (resultOpt.isPresent()) {
			return resultOpt.get();
		}
		return null;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public TicketEntity save(TicketEntity entity) {
		Boolean isClosed = true;
		Boolean hasDraft = false;
		for (TicketItemEntity item : entity.getItems()) {
			if (isClosed && !item.getStatus().equals(TicketStatus.CLOSED.toString())) {
				isClosed = false;
			}
			if (!hasDraft && !item.getStatus().equals(TicketStatus.DRAFT.toString())) {
				hasDraft = true;
			}
		}
		entity.setIsClosed(isClosed);
		entity.setHasDraft(hasDraft);
		TicketEntity result = ticketRepository.save(entity);
		return result;
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
				_LOGGER.info(String.format("Uploading image - %s to s3", fileName));
				Object response = uploadService.uploadFile(bucketName, subDirectory, file.getBytes(), fileName);
				uploadedFiles.add(ParamsUtils.getParam("CLOUDFRONT_URL").concat("/").concat(response.toString()));
			}
			return uploadedFiles;
		} catch (IOException err) {
			_LOGGER.error("Files Uploading ", err);
			throw new ValidationException(new ErrorBean(Errors.UPDATE_FAILED, "Error while uploading images", "images"));
		}
	}

	public List<TicketEntity> findByReferenceIdAndStatusInAndActive(String referenceId, List<String> statuses, Integer active) {
		List<TicketEntity> tickets = ticketRepository.findByReferenceIdAndStatusInAndActive(referenceId, statuses, active);
		if (referenceId != null) {
			if (tickets.isEmpty()) {
				tickets.add(null);
			}
		}
		return tickets;
	}

	public PageAndSortResult<TicketEntity> getAllSingularTicketsPaginated(Integer pageSize, Integer pageNo, Map<String, Object> filters,
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

	/*
	public PageAndSortResult<String> getAllGroupedTicketsPaginated(Integer pageSize, Integer pageNo, Map<String, Object> filters,
			Map<String, PageAndSortRequest.SortDirection> sort) {
		PageAndSortResult<String> tickets = null;
		try {
			tickets = ticketRepository.findGroupedByReferenceId(List.of(TicketStatus.DRAFT.toString(), TicketStatus.IN_PROGRESS.toString()), 2, 0, 1);
		} catch (Exception e) {
			_LOGGER.error(e);
			throw new ValidationException(ErrorBean.withError("FETCH_ERROR", e.getMessage(), null));
		}
		return tickets;
	}
	 */

	@Override
	public Class<TicketEntity> getEntity() {
		return TicketEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return ticketRepository;
	}
}