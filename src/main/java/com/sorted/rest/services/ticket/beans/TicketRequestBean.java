package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class TicketRequestBean implements Serializable {

	private static final long serialVersionUID = 4599413762862017279L;

	private UserDetail internalUserDetail;

	private UserDetail requesterUserDetail;

	private Map<String, WhSkuResponse> whSkuResponseMap;

	private FranchiseOrderResponseBean orderResponse;

	private Map<String, FranchiseOrderItemResponseBean> orderItemSkuMap;

	private StoreReturnResponseBean storeReturnResponse;

	private Map<String, StoreReturnItemData> storeReturnItemSkuMap;

	private StoreDataResponse storeDataResponse;

	private UserDetail amUserResponse;

	//	todo: tickets for PAYMENT_ISSUE with referenceId not allowed in V1, add in subsequent releases
	//	private WalletStatementBean walletStatementBean;
}
