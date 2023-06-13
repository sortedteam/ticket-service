package com.sorted.rest.services.ticket.clients;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ServerException;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.services.ticket.beans.*;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ClientService {

	AppLogger _LOGGER = LoggingManager.getLogger(ClientService.class);

	@Autowired
	private StoreClient storeClient;

	@Autowired
	private AuthConsumerClient authConsumerClient;

	@Autowired
	private OrderClient orderClient;

	@Autowired
	private PaymentClient paymentClient;

	@Autowired
	private WmsClient wmsClient;

	@Autowired
	private WidgetClient widgetClient;

	@Value("${client.wms.auth_key}")
	@Getter
	private String RZ_AUTH_VALUE;

	public UserServiceResponse getUserDetailsFromCustomerId(UUID customerId) {
		String customerIdString = customerId.toString();
		Map<String, Object> headerMap = new HashMap<>();
		try {
			return authConsumerClient.getUserDetailsFromCustomerId(headerMap, customerIdString);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while getting userDetails for customerId  ", customerIdString), e);
			return null;
		}
	}

	public String getFilteredOrDefaultAudience(String entityType, String entityId, List<String> superset, String defaultAudience) {
		String audience = null;
		try {
			audience = widgetClient.getFilteredAudience(TicketConstants.TICKET_RAISING_USER_USERTYPE, entityType, entityId, superset);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while fetching filteredOrDefault audience for %s : %s from subset : %s with default : %s", entityType, entityId,
					superset, defaultAudience), e);
		}
		return audience == null ? defaultAudience : audience;
	}

	public FranchiseOrderResponseBean getFranchiseOrderInfo(UUID orderId, String storeId) {
		FranchiseOrderResponseBean orderResponseBean = null;
		try {
			Map<String, Object> headerMap = new HashMap<>();
			headerMap.put("storeId", storeId);
			orderResponseBean = orderClient.getFranchiseOrderInfo(headerMap, orderId);
			return orderResponseBean;
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while getting Franchise Order Info for orderId ", orderId), e);
			throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetch order data"));
		}
	}

	public List<WhSkuResponse> getStoreSkuInventoryForBulkRequest(Set<String> skuCodes, String storeId) {
		try {
			Map<String, Object> headerMap = new HashMap<>();
			headerMap.put("rz-auth-key", RZ_AUTH_VALUE);
			return wmsClient.getStoreSkuInventoryForBulkRequest(headerMap, skuCodes, storeId);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching StoreInventory", e);
			throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetch data from warehouse"));
		}
	}

	public List<WalletStatementBean> fetchWalletStatementByTxnDetail(String txnDetail) {
		try {
			return paymentClient.fetchWalletStatementByTxnDetail(txnDetail);
		} catch (Exception e) {
			_LOGGER.error("Error while Fetching wallet statement", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching wallet statement."));
		}
	}

	public StoreReturnResponseBean getStoreReturnByOrderId(String orderId) {
		try {
			Map<String, Object> headerMap = new HashMap<>();
			headerMap.put("rz-auth-key", RZ_AUTH_VALUE);
			return wmsClient.getStoreReturnByOrderId(headerMap, orderId);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching StoreReturn", e);
			throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetch data from warehouse"));
		}
	}
}
