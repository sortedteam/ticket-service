package com.sorted.rest.services.ticket.beans;

import com.sorted.rest.services.ticket.constants.TicketConstants;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class TicketRequestBean implements Serializable {

	private static final long serialVersionUID = 4599413762862017279L;

	private UserDetail internalUserDetail;

	private Map<String, WhSkuResponse> whSkuResponseMap;

	private FranchiseOrderResponseBean orderResponse;

	private Map<String, FranchiseOrderItemResponseBean> orderItemSkuMap;

	private BigDecimal storeCategoryRefundPermissibilityFactor = TicketConstants.DEFAULT_STORE_CATEGORY_REFUND_PERMISSIBILITY_FACTOR;

	private List<WalletStatementBean> walletStatementBeans;
}
