package com.sorted.rest.services.ticket.utils;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.services.params.service.ParamService;
import com.sorted.rest.services.ticket.beans.*;
import com.sorted.rest.services.ticket.clients.TicketClientService;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import com.sorted.rest.services.ticket.constants.TicketConstants.EntityType;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketCategoryRoot;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TicketRequestUtils {

	@Autowired
	private ParamService paramService;

	@Autowired
	private TicketClientService ticketClientService;

	@Autowired
	private UserUtils userUtils;

	private static ThreadLocal<TicketRequestBean> MEMORY_THREAD_LOCAL = new ThreadLocal<>();

	public void setTicketRequest(TicketRequestBean bean) {
		MEMORY_THREAD_LOCAL.set(bean);
	}

	public TicketRequestBean getTicketRequest() {
		TicketRequestBean ticketRequestBean = MEMORY_THREAD_LOCAL.get();
		return ticketRequestBean != null ? ticketRequestBean : new TicketRequestBean();
	}

	public void clearTicketRequest() {
		MEMORY_THREAD_LOCAL.remove();
	} // to do after API call finishes

	public void populateTicketRequestAsPerCategoryRoot(TicketEntity requestTicket, List<TicketItemEntity> requestTicketItems) {
		TicketRequestBean ticketRequestBean = new TicketRequestBean();
		String categoryRootLabel = requestTicket.getCategoryRoot().getLabel();
		String entityType = requestTicket.getRequesterEntityType();
		if (entityType.equals(EntityType.STORE.toString())) {
			String storeId = requestTicket.getRequesterEntityId();
			if (requestTicket.getMetadata().getStoreDetails() == null) {
				ticketRequestBean.setStoreDataResponse(ticketClientService.getStoreDataFromId(storeId));
				requestTicket.getMetadata().setStoreDetails(StoreDetailsBean.newInstance());
			}
			if (categoryRootLabel.equals(TicketCategoryRoot.ORDER_ISSUE.toString())) {
				StoreReturnResponseBean storeReturnResponseBean = ticketClientService.getStoreReturnByOrderId(requestTicket.getReferenceId());
				Map<String, StoreReturnItemData> storeReturnItemSkuMap = new HashMap<>();
				if (storeReturnResponseBean != null) {
					ticketRequestBean.setStoreReturnResponse(storeReturnResponseBean);
					for (StoreReturnItemData storeReturnItemData : storeReturnResponseBean.getStoreReturnItemDataList()) {
						if (!storeReturnItemSkuMap.containsKey(storeReturnItemData.getSkuCode())) {
							storeReturnItemSkuMap.put(storeReturnItemData.getSkuCode(), storeReturnItemData);
						}
					}
				}
				ticketRequestBean.setStoreReturnItemSkuMap(storeReturnItemSkuMap);

				if (requestTicket.getHasNew()) {
					UUID orderId = UUID.fromString(requestTicket.getReferenceId());
					FranchiseOrderResponseBean orderResponseBean = ticketClientService.getFranchiseOrderInfo(orderId, storeId);
					ticketRequestBean.setOrderResponse(orderResponseBean);
					if (validateTicketCreationWindow(orderResponseBean, ticketRequestBean.getStoreDataResponse())) {
						throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Ticket Creation window has been closed for this order", ""));
					}

					Map<String, FranchiseOrderItemResponseBean> orderItemSkuMap = new HashMap<>();
					for (FranchiseOrderItemResponseBean orderItem : orderResponseBean.getOrderItems()) {
						orderItemSkuMap.put(orderItem.getSkuCode(), orderItem);
					}
					ticketRequestBean.setOrderItemSkuMap(orderItemSkuMap);

					Set<String> skuCodes = requestTicketItems.stream().filter(item -> item.getDetails().getOrderDetails() != null)
							.map(item -> item.getDetails().getOrderDetails().getSkuCode())
							.filter(skuCode -> !StringUtils.isEmpty(skuCode) && !StringUtils.isEmpty(skuCode.trim())).collect(Collectors.toSet());
					if (skuCodes.isEmpty()) {
						ticketRequestBean.setWhSkuResponseMap(new HashMap<>());
					} else {
						List<WhSkuResponse> whSkuResponse = ticketClientService.getStoreSkuInventoryForBulkRequest(skuCodes, storeId);
						Map<String, WhSkuResponse> whSkuResponseMap = new HashMap<>();
						for (WhSkuResponse whSku : whSkuResponse) {
							whSkuResponseMap.put(whSku.getSkuCode(), whSku);
						}
						ticketRequestBean.setWhSkuResponseMap(whSkuResponseMap);

						if (requestTicket.getMetadata().getStoreDetails().getRefundPermissibilityFactor() == null) {
							Map<String, BigDecimal> storeCategoryRefundPermissibilityMap = Arrays.stream(
											paramService.getParam("STORE_CATEGORY_REFUND_PERMISSIBILITY", "GOOD:1|BAD:0.5|UGLY:0").split("\\|"))
									.map(s -> s.split(":", 2)).collect(Collectors.toMap(s -> s[0], s -> s.length > 1 ?
											BigDecimal.valueOf(Double.valueOf(s[1])) :
											TicketConstants.DEFAULT_STORE_CATEGORY_REFUND_PERMISSIBILITY_FACTOR));
							String storeCategory = requestTicket.getRequesterEntityCategory();
							if (storeCategory != null && storeCategoryRefundPermissibilityMap.containsKey(storeCategory)) {
								requestTicket.getMetadata().getStoreDetails()
										.setRefundPermissibilityFactor(storeCategoryRefundPermissibilityMap.get(storeCategory));
							}
						}
					}
					if (requestTicket.getMetadata().getStoreDetails().getRefundPermissibilityFactor() == null) {
						requestTicket.getMetadata().getStoreDetails()
								.setRefundPermissibilityFactor(TicketConstants.DEFAULT_STORE_CATEGORY_REFUND_PERMISSIBILITY_FACTOR);
					}
				}
				//			todo: tickets for PAYMENT_ISSUE with referenceId not allowed in V1, add in subsequent releases
				//			} else if (categoryRootLabel.equals(TicketCategoryRoot.PAYMENT_ISSUE.toString())) {
				//				if (!StringUtils.isEmpty(ticket.getReferenceId())) {
				//					WalletStatementBean walletStatementBean = clientService.fetchWalletStatementById(Integer.parseInt(ticket.getReferenceId()));
				//					ticketRequestBean.setWalletStatementBean(walletStatementBean);
				//				}
			}
		}
		ticketRequestBean.setInternalUserDetail(userUtils.getInternalUserDetail());
		ticketRequestBean.setRequesterUserDetail(userUtils.getUserDetail(SessionUtils.getAuthUserId()));
		setTicketRequest(ticketRequestBean);
	}

	private boolean validateTicketCreationWindow(FranchiseOrderResponseBean orderResponseBean, StoreDataResponse storeDataResponse) {
		LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
		String ticketClosingHours = ParamsUtils.getParam("TICKET_CLOSING_HOURS", "17:00");
		LocalTime ticketClosingTime = convertToLocalTime(ticketClosingHours, DateTimeFormatter.ofPattern("HH:mm"));
		if (orderResponseBean.getMetadata() == null || orderResponseBean.getMetadata()
				.getDeliveryDetails() == null || storeDataResponse == null || storeDataResponse.getOpenTime() == null) {
			return currentTime.toLocalTime().isAfter(ticketClosingTime);
		}
		LocalDateTime deliveryCompletionTime = getDeliveryCompletionTime(orderResponseBean);
		LocalTime storeOpenTime = convertToLocalTime(storeDataResponse.getOpenTime(), DateTimeFormatter.ofPattern("HH:mm"));
		LocalTime maxTime = deliveryCompletionTime.toLocalTime().isAfter(storeOpenTime) ? deliveryCompletionTime.toLocalTime() : storeOpenTime;
		Integer bufferHours = Integer.valueOf(ParamsUtils.getParam("TICKET_BUFFER_HOURS", "3"));
		maxTime = maxTime.plusHours(bufferHours);

		return currentTime.toLocalTime().isAfter(maxTime);
	}

	private LocalDateTime getDeliveryCompletionTime(FranchiseOrderResponseBean orderResponseBean) {
		if (orderResponseBean.getMetadata() != null && CollectionUtils.isNotEmpty(orderResponseBean.getMetadata().getDeliveryDetails())) {
			List<FranchiseOrderDeliveryBean> deliveryDetails = orderResponseBean.getMetadata().getDeliveryDetails();
			LocalDateTime deliveryCompletionTime = convertToLocalDateTime(deliveryDetails.get(0).getCompletedAt(),
					DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
			if (deliveryDetails.size() > 1) {
				for (int i = 1; i < deliveryDetails.size(); i++) {
					LocalDateTime formattedCompletionTime = convertToLocalDateTime(deliveryDetails.get(i).getCompletedAt(),
							DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
					if (formattedCompletionTime.isAfter(deliveryCompletionTime)) {
						deliveryCompletionTime = formattedCompletionTime;
					}
				}
			}
			return deliveryCompletionTime;
		}
		return null;
	}

	private LocalDateTime convertToLocalDateTime(String completionTime, DateTimeFormatter formatter) {
		return LocalDateTime.parse(completionTime, formatter);
	}

	private LocalTime convertToLocalTime(String completionTime, DateTimeFormatter formatter) {
		return LocalTime.parse(completionTime, formatter);
	}

}
