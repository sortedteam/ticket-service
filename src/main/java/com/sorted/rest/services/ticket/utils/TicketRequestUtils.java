package com.sorted.rest.services.ticket.utils;

import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.services.params.service.ParamService;
import com.sorted.rest.services.ticket.beans.*;
import com.sorted.rest.services.ticket.clients.ClientService;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import com.sorted.rest.services.ticket.constants.TicketConstants.EntityType;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketCategoryRoot;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TicketRequestUtils {

	@Autowired
	private ParamService paramService;

	@Autowired
	private ClientService clientService;

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

	public void populateTicketRequestAsPerCategoryRoot(TicketEntity ticket) {
		TicketRequestBean ticketRequestBean = new TicketRequestBean();
		String categoryRootLabel = ticket.getCategoryRoot().getLabel();
		String entityType = ticket.getRequesterEntityType();
		if (entityType.equals(EntityType.STORE.toString())) {
			String storeId = ticket.getRequesterEntityId();
			ticketRequestBean.setStoreDataResponse(clientService.getStoreDataFromId(storeId));
			if (categoryRootLabel.equals(TicketCategoryRoot.ORDER_ISSUE.toString())) {
				UUID orderId = UUID.fromString(ticket.getReferenceId());
				FranchiseOrderResponseBean orderResponseBean = clientService.getFranchiseOrderInfo(orderId, storeId);
				ticketRequestBean.setOrderResponse(orderResponseBean);

				Map<String, FranchiseOrderItemResponseBean> orderItemSkuMap = new HashMap<>();
				for (FranchiseOrderItemResponseBean orderItem : orderResponseBean.getOrderItems()) {
					orderItemSkuMap.put(orderItem.getSkuCode(), orderItem);
				}
				ticketRequestBean.setOrderItemSkuMap(orderItemSkuMap);

				StoreReturnResponseBean storeReturnResponseBean = clientService.getStoreReturnByOrderId(ticket.getReferenceId());
				ticketRequestBean.setStoreReturnResponse(storeReturnResponseBean);

				Map<String, StoreReturnItemData> storeReturnItemSkuMap = new HashMap<>();
				for (StoreReturnItemData storeReturnItemData : storeReturnResponseBean.getStoreReturnItemDataList()) {
					// if skuCode is not unique in storeReturnItemDataList, use the first one
					if (!storeReturnItemSkuMap.containsKey(storeReturnItemData.getSkuCode())) {
						storeReturnItemSkuMap.put(storeReturnItemData.getSkuCode(), storeReturnItemData);
					}
				}
				ticketRequestBean.setStoreReturnItemSkuMap(storeReturnItemSkuMap);

				Set<String> skuCodes = ticket.getItems().stream().filter(item -> item.getResolutionDetails().getOrderDetails() != null)
						.map(item -> item.getResolutionDetails().getOrderDetails().getSkuCode())
						.filter(skuCode -> !StringUtils.isEmpty(skuCode) && !StringUtils.isEmpty(skuCode.trim())).collect(Collectors.toSet());
				if (skuCodes.isEmpty()) {
					ticketRequestBean.setWhSkuResponseMap(new HashMap<>());
				} else {
					List<WhSkuResponse> whSkuResponse = clientService.getStoreSkuInventoryForBulkRequest(skuCodes, storeId);
					Map<String, WhSkuResponse> whSkuResponseMap = new HashMap<>();
					for (WhSkuResponse whSku : whSkuResponse) {
						whSkuResponseMap.put(whSku.getSkuCode(), whSku);
					}
					ticketRequestBean.setWhSkuResponseMap(whSkuResponseMap);

					Map<String, BigDecimal> storeCategoryRefundPermissibilityMap = Arrays.stream(
									paramService.getParam("STORE_CATEGORY_REFUND_PERMISSIBILITY", "Green:1|Red:1|Yellow:1").split("\\|")).map(s -> s.split(":", 2))
							.collect(Collectors.toMap(s -> s[0], s -> s.length > 1 ?
									BigDecimal.valueOf(Double.valueOf(s[1])) :
									TicketConstants.DEFAULT_STORE_CATEGORY_REFUND_PERMISSIBILITY_FACTOR));
					String storeCategory = ticket.getRequesterEntityCategory();
					if (storeCategory != null && storeCategoryRefundPermissibilityMap.containsKey(storeCategory)) {
						ticketRequestBean.setStoreCategoryRefundPermissibilityFactor(storeCategoryRefundPermissibilityMap.get(storeCategory));
					}
				}
			} else if (categoryRootLabel.equals(TicketCategoryRoot.PAYMENT_ISSUE.toString())) {
				if (!StringUtils.isEmpty(ticket.getReferenceId())) {
					WalletStatementBean walletStatementBean = clientService.fetchWalletStatementById(Integer.parseInt(ticket.getReferenceId()));
					ticketRequestBean.setWalletStatementBean(walletStatementBean);
				}
			}
		}
		ticketRequestBean.setInternalUserDetail(userUtils.getInternalUserDetail());
		ticketRequestBean.setRequesterUserDetail(userUtils.getUserDetail(SessionUtils.getAuthUserId()));
		setTicketRequest(ticketRequestBean);
	}

}
