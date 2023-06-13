package com.sorted.rest.services.ticket.utils;

import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.services.params.service.ParamService;
import com.sorted.rest.services.ticket.beans.*;
import com.sorted.rest.services.ticket.clients.ClientService;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import com.sorted.rest.services.ticket.constants.TicketConstants.EntityType;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketCategoryRoot;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

	@Value("${auth.id}")
	private UUID internalAuthUserId;

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

	public void populateTicketRequestAsPerCategoryRoot(List<TicketEntity> tickets) {
		TicketRequestBean ticketRequestBean = new TicketRequestBean();
		String categoryRootLabel = tickets.get(0).getCategoryRoot().getLabel();
		String entityType = tickets.get(0).getRequesterEntityType();
		if (entityType.equals(EntityType.STORE.toString())) {
			if (categoryRootLabel.equals(TicketCategoryRoot.ORDER_ISSUE.toString())) {
				String storeId = tickets.get(0).getRequesterEntityId();
				if (!StringUtils.isEmpty(tickets.get(0).getReferenceId())) {
					UUID orderId = UUID.fromString(tickets.get(0).getReferenceId());
					FranchiseOrderResponseBean orderResponseBean = clientService.getFranchiseOrderInfo(orderId, storeId);
					ticketRequestBean.setOrderResponse(orderResponseBean);

					Map<String, FranchiseOrderItemResponseBean> orderItemSkuMap = new HashMap<>();
					for (FranchiseOrderItemResponseBean orderItem : orderResponseBean.getOrderItems()) {
						orderItemSkuMap.put(orderItem.getSkuCode(), orderItem);
					}
					ticketRequestBean.setOrderItemSkuMap(orderItemSkuMap);

					StoreReturnResponseBean storeReturnResponseBean = clientService.getStoreReturnByOrderId(tickets.get(0).getReferenceId());
					ticketRequestBean.setStoreReturnResponse(storeReturnResponseBean);

					Map<String, StoreReturnItemData> storeReturnItemSkuMap = new HashMap<>();
					for (StoreReturnItemData storeReturnItemData : storeReturnResponseBean.getStoreReturnItemDataList()) {
						// redo map if skuCode is not unique in storeReturnItemDataList
						storeReturnItemSkuMap.put(storeReturnItemData.getSkuCode(), storeReturnItemData);
					}
					ticketRequestBean.setStoreReturnItemSkuMap(storeReturnItemSkuMap);
				}
				Set<String> skuCodes = tickets.stream().filter(ticket -> ticket.getDetails().getOrderDetails() != null)
						.map(ticket -> ticket.getDetails().getOrderDetails().getSkuCode())
						.filter(skuCode -> !StringUtils.isEmpty(skuCode) && !StringUtils.isEmpty(skuCode.trim())).collect(Collectors.toSet());
				if (!skuCodes.isEmpty()) {
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
					String storeCategory = tickets.get(0).getRequesterEntityCategory();
					if (storeCategory != null && storeCategoryRefundPermissibilityMap.containsKey(storeCategory)) {
						ticketRequestBean.setStoreCategoryRefundPermissibilityFactor(storeCategoryRefundPermissibilityMap.get(storeCategory));
					}
				}
			} else if (categoryRootLabel.equals(TicketCategoryRoot.PAYMENT_ISSUE.toString())) {
				if (tickets.get(0).getDetails().getPaymentDetails() != null && !StringUtils.isEmpty(
						tickets.get(0).getDetails().getPaymentDetails().getTxnDetail())) {
					List<WalletStatementBean> walletStatementBeans = clientService.fetchWalletStatementByTxnDetail(
							tickets.get(0).getDetails().getPaymentDetails().getTxnDetail());
					for (WalletStatementBean walletStatementBean : walletStatementBeans) {
						if (walletStatementBean != null && walletStatementBean.getCreatedAt() != null) {
							walletStatementBean.setCreatedAt(DateUtils.convertDateUtcToIst(walletStatementBean.getCreatedAt()));
						}
					}
					ticketRequestBean.setWalletStatementBeans(walletStatementBeans);
				}
			}
		}
		ticketRequestBean.setInternalUserDetail(userUtils.getUserDetail(internalAuthUserId));
		ticketRequestBean.setRequesterUserDetail(userUtils.getUserDetail(SessionUtils.getAuthUserId()));
		setTicketRequest(ticketRequestBean);
	}

}
