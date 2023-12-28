package com.sorted.rest.services.ticket.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ServerException;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.ticket.beans.*;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import com.sorted.rest.services.ticket.constants.TicketConstants.EntityType;
import feign.FeignException.FeignClientException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TicketClientService {

	AppLogger _LOGGER = LoggingManager.getLogger(TicketClientService.class);

	@Autowired
	private TicketStoreClient ticketStoreClient;

	@Autowired
	private TicketAuthConsumerClient ticketAuthClient;

	@Autowired
	private TicketOrderClient ticketOrderClient;

	@Autowired
	private TicketPaymentClient ticketPaymentClient;

	@Autowired
	private TicketWmsClient ticketWmsClient;

	@Autowired
	private TicketWidgetClient ticketWidgetClient;

	@Autowired
	private TicketOfferClient ticketOfferClient;

	@Value("${client.wms.auth_key}")
	@Getter
	private String RZ_AUTH_VALUE;

	@Autowired
	private BaseMapper<?, ?> mapper;

	public UserServiceResponse getUserDetailsFromCustomerId(UUID customerId) {
		String customerIdString = customerId.toString();
		Map<String, Object> headerMap = new HashMap<>();
		try {
			return ticketAuthClient.getUserDetailsFromCustomerId(headerMap, customerIdString);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while getting userDetails for customerId  ", customerIdString), e);
			return null;
		}
	}

	public String getFilteredOrDefaultAudience(EntityType entityType, String entityId, List<String> superset, String defaultAudience) {
		String audience = null;
		try {
			audience = ticketWidgetClient.getFilteredAudience(TicketConstants.TICKET_RAISING_USER_USERTYPE, entityType.toString(), entityId, superset);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while fetching filteredOrDefault audience for %s : %s from subset : %s with default : %s", entityType.toString(),
					entityId, superset, defaultAudience), e);
		}
		return audience == null ? defaultAudience : audience;
	}

	public StoreDataResponse getStoreDataFromId(String storeId) {
		try {
			StoreDataResponse response = ticketStoreClient.getStoreDataFromId(storeId).get(0);
			if (response == null || response.getId() == null) {
				throw new ValidationException(new ErrorBean(Errors.NO_DATA_FOUND, "We are unable to locate the store"));
			}
			return response;
		} catch (FeignClientException f) {
			ErrorBean error = new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching store data", null);
			try {
				error = mapper.getJacksonMapper().readValue(f.contentUTF8(), ErrorBean.class);
				error.setCode(Errors.SERVER_EXCEPTION);
			} catch (JsonProcessingException e) {
				_LOGGER.error("Error while converting feign client error bean ", e);
			}
			throw new ValidationException(error);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching StoreFromId ", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching store data"));
		}
	}

	public FranchiseOrderResponseBean getFranchiseOrderInfo(UUID orderId, String storeId) {
		FranchiseOrderResponseBean orderResponseBean = null;
		try {
			Map<String, Object> headerMap = new HashMap<>();
			headerMap.put("storeId", storeId);
			orderResponseBean = ticketOrderClient.getFranchiseOrderInfo(headerMap, orderId);
			return orderResponseBean;
		} catch (FeignClientException f) {
			ErrorBean error = new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching order data", null);
			try {
				error = mapper.getJacksonMapper().readValue(f.contentUTF8(), ErrorBean.class);
				error.setCode(Errors.SERVER_EXCEPTION);
			} catch (JsonProcessingException e) {
				_LOGGER.error("Error while converting feign client error bean ", e);
			}
			throw new ValidationException(error);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while getting Franchise Order Info for orderId : %s", orderId), e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching order data"));
		}
	}

	public List<WhSkuResponse> getStoreSkuInventoryForBulkRequest(Set<String> skuCodes, String storeId) {
		try {
			Map<String, Object> headerMap = new HashMap<>();
			headerMap.put("rz-auth-key", RZ_AUTH_VALUE);
			return ticketWmsClient.getStoreSkuInventoryForBulkRequest(headerMap, skuCodes, storeId, true);
		} catch (FeignClientException f) {
			ErrorBean error = new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching while fetching wh skus data from warehouse", null);
			try {
				error = mapper.getJacksonMapper().readValue(f.contentUTF8(), ErrorBean.class);
				error.setCode(Errors.SERVER_EXCEPTION);
			} catch (JsonProcessingException e) {
				_LOGGER.error("Error while converting feign client error bean ", e);
			}
			throw new ValidationException(error);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching StoreInventory", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching wh skus data from warehouse"));
		}
	}

	public WalletStatementBean fetchWalletStatementById(Integer id) {
		try {
			return ticketPaymentClient.fetchWalletStatementById(id);
		} catch (FeignClientException f) {
			ErrorBean error = new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching wallet statement", null);
			try {
				error = mapper.getJacksonMapper().readValue(f.contentUTF8(), ErrorBean.class);
				error.setCode(Errors.SERVER_EXCEPTION);
			} catch (JsonProcessingException e) {
				_LOGGER.error("Error while converting feign client error bean ", e);
			}
			throw new ValidationException(error);
		} catch (Exception e) {
			_LOGGER.error("Error while Fetching wallet statement", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching wallet statement"));
		}
	}

	public StoreReturnResponseBean getStoreReturnByOrderId(String orderId) {
		try {
			Map<String, Object> headerMap = new HashMap<>();
			headerMap.put("rz-auth-key", RZ_AUTH_VALUE);
			return ticketWmsClient.getStoreReturnByOrderId(headerMap, orderId);
		} catch (FeignClientException f) {
			ErrorBean error = new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching store return", null);
			try {
				error = mapper.getJacksonMapper().readValue(f.contentUTF8(), ErrorBean.class);
				error.setCode(Errors.SERVER_EXCEPTION);
			} catch (JsonProcessingException e) {
				_LOGGER.error("Error while converting feign client error bean ", e);
			}
			throw new ValidationException(error);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching StoreReturn", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching store return"));
		}
	}

	public List<FranchiseOrderListBean> getFranchiseOrderByDisplayIds(Set<String> ids) {
		try {
			return ticketOrderClient.getFranchiseOrderByDisplayIds(ids);
		} catch (FeignClientException f) {
			ErrorBean error = new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching order data by display ids", null);
			try {
				error = mapper.getJacksonMapper().readValue(f.contentUTF8(), ErrorBean.class);
				error.setCode(Errors.SERVER_EXCEPTION);
			} catch (JsonProcessingException e) {
				_LOGGER.error("Error while converting feign client error bean ", e);
			}
			throw new ValidationException(error);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while getting Franchise Order Info for displayOrderIds : %s", ids), e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching order data by display ids"));
		}
	}

	public FranchiseOrderResponseBean imsProcessFranchiseRefundOrder(ImsFranchiseOrderRefundBean request, String key) {
		try {
			return ticketOrderClient.imsProcessFranchiseRefundOrder(request, key);
		} catch (FeignClientException f) {
			ErrorBean error = new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while processing order refund", null);
			try {
				error = mapper.getJacksonMapper().readValue(f.contentUTF8(), ErrorBean.class);
				error.setCode(Errors.SERVER_EXCEPTION);
			} catch (JsonProcessingException e) {
				_LOGGER.error("Error while converting feign client error bean ", e);
			}
			throw new ValidationException(error);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while refunding with request : %s ", request), e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while processing order refund"));
		}
	}

	public FranchiseOrderResponseBean imsProcessFranchiseRefundAllOrder(ImsFranchiseOrderRefundAllBean request, String key) {
		try {
			return ticketOrderClient.imsProcessFranchiseRefundAllOrder(request, key);
		} catch (FeignClientException f) {
			ErrorBean error = new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while processing full order refund", null);
			try {
				error = mapper.getJacksonMapper().readValue(f.contentUTF8(), ErrorBean.class);
				error.setCode(Errors.SERVER_EXCEPTION);
			} catch (JsonProcessingException e) {
				_LOGGER.error("Error while converting feign client error bean ", e);
			}
			throw new ValidationException(error);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while refunding with request : %s ", request), e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while processing full order refund"));
		}
	}

	public Set<String> getMappedStores(UUID userId) {
		List<UserStoreMappingResponse> response = null;
		try {
			response = ticketAuthClient.getUserStoreMappingFromUserId(userId.toString());
			if (response == null || response.isEmpty()) {
				throw new ValidationException(new ErrorBean("user_store_mapping_not_found", "There are no active store mapped to user id :" + userId));
			}

		} catch (Exception e) {
			_LOGGER.error("Error while fetching getMappedStores", e);
			if (e instanceof ValidationException) {
				throw e;
			} else {
				throw new ValidationException(new ErrorBean("user_store_mapping_not_found", "Something went wrong. Kindly try again."));
			}
		}
		Set<String> mappedStores = response.stream().map(UserStoreMappingResponse::getStoreId).collect(Collectors.toSet());
		return mappedStores;
	}

	public void giveTargetCashbackForStoreIdAndDate(String requesterEntityId, Date deliveryDate) {
		TargetCashbackCronRequest targetCashbackCronRequest = new TargetCashbackCronRequest();
		targetCashbackCronRequest.setDate((new SimpleDateFormat("yyyy-MM-dd")).format(deliveryDate));
		targetCashbackCronRequest.setStoreIds(List.of(requesterEntityId));
		_LOGGER.info(String.format("Cashback Cron Triggered for store : %s ", requesterEntityId));
		ticketOfferClient.runDailyCashbackCronForStoreIdAndDate(targetCashbackCronRequest);
	}

	public FranchiseOrderResponseBean cancelFranchiseOrderPostBilling(FranchiseOrderCancelPostBillingRequest request, String key, UUID orderId) {
		try {
			return ticketOrderClient.cancelFranchiseOrderPostBilling(request, key, orderId);
		} catch (FeignClientException f) {
			_LOGGER.error("Something went wrong while cancelling full order", f);
			ErrorBean error = new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while cancelling full order", null);
			try {
				error = mapper.getJacksonMapper().readValue(f.contentUTF8(), ErrorBean.class);
				error.setCode(Errors.SERVER_EXCEPTION);
			} catch (JsonProcessingException e) {
				_LOGGER.error("Error while converting feign client error bean ", e);
			}
			throw new ValidationException(error);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while cancelling full order with request : %s ", request), e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while cancelling full order"));
		}
	}

	public UserDetail getAmMappedToStore(String storeId) {
		UserDetail response = null;
		try {
			List<AmStoreMappingResponse> amMappedStores = ticketAuthClient.getAmMappedStores(storeId, true);
			if (amMappedStores == null || amMappedStores.isEmpty()) {
				throw new ValidationException(new ErrorBean(Errors.NO_DATA_FOUND, "There are no am mapped to this store :" + storeId));
			}
			if (amMappedStores.get(0).getAmUser() == null) {
				throw new ValidationException(new ErrorBean(Errors.NO_DATA_FOUND, "Am user info not found for store :" + storeId));
			}
			response = mapper.mapSrcToDest(amMappedStores.get(0).getAmUser(), UserDetail.newInstance());
		} catch (Exception e) {
			_LOGGER.error("Error while fetching getMappedStores", e);
		}
		return response;
	}

	public ConsumerOrderResponseBean imsProcessConsumerRefundOrder(ImsConsumerOrderRefundBean request, String key) {
		try {
			return ticketOrderClient.imsConsumerFranchiseRefundOrder(request, key);
		} catch (FeignClientException f) {
			ErrorBean error = new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while processing consumer order refund", null);
			try {
				error = mapper.getJacksonMapper().readValue(f.contentUTF8(), ErrorBean.class);
				error.setCode(Errors.SERVER_EXCEPTION);
			} catch (JsonProcessingException e) {
				_LOGGER.error("Error while converting feign client error bean ", e);
			}
			throw new ValidationException(error);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while refunding with request : %s ", request), e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while processing consumer order refund"));
		}
	}
}
