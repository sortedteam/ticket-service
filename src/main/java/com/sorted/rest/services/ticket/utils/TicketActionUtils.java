package com.sorted.rest.services.ticket.utils;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.services.ticket.actions.AutomaticOrderRefundAction;
import com.sorted.rest.services.ticket.actions.EscalateToTeamAction;
import com.sorted.rest.services.ticket.actions.TicketActionsInterface;
import com.sorted.rest.services.ticket.beans.*;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import com.sorted.rest.services.ticket.constants.TicketConstants.EntityType;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketCategoryRoot;
import com.sorted.rest.services.ticket.constants.TicketConstants.TicketResolutionTeam;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Component
public class TicketActionUtils {

	static AppLogger _LOGGER = LoggingManager.getLogger(TicketActionUtils.class);

	@Autowired
	private TicketHistoryService ticketHistoryService;

	@Autowired
	private TicketRequestUtils ticketRequestUtils;

	@Autowired
	private EscalateToTeamAction escalateToTeamAction;

	@Autowired
	private AutomaticOrderRefundAction automaticOrderRefundAction;

	public void invokeTicketCreateAction(TicketEntity ticket) {
		TicketActionDetailsBean defaultActionDetails = TicketActionDetailsBean.newInstance();
		defaultActionDetails.setUserDetail(ticketRequestUtils.getTicketRequest().getRequesterUserDetail());
		defaultActionDetails.setRemarks(TicketConstants.NEW_TICKET_CREATED_REMARKS);
		ticketHistoryService.addTicketHistory(ticket.getId(), TicketConstants.NEW_TICKET_CREATED_ACTION, defaultActionDetails);
	}

	public void invokeTicketRaiseAction(TicketEntity ticket) {
		List<String> actions = ticket.getCategoryLeaf().getOnCreateActions();
		Boolean terminate = false;
		TicketActionDetailsBean defaultActionDetails = TicketActionDetailsBean.newInstance();
		defaultActionDetails.setUserDetail(ticketRequestUtils.getTicketRequest().getInternalUserDetail());
		for (String action : actions) {
			TicketActionsInterface ticketAction = null;
			if (action.equals(TicketConstants.AUTOMATIC_ORDER_REFUND_ACTION)) {
				ticketAction = automaticOrderRefundAction;
			} else if (action.equals(TicketConstants.ESCALATE_TO_WAREHOUSE_ACTION)) {
				ticketAction = escalateToTeamAction;
				escalateToTeamAction.setTeamAndRemarks(TicketResolutionTeam.WAREHOUSE, TicketConstants.ESCALATE_TO_WAREHOUSE_REMARKS);
			} else if (action.equals(TicketConstants.ESCALATE_TO_CUSTOMERCARE_ACTION)) {
				ticketAction = escalateToTeamAction;
				escalateToTeamAction.setTeamAndRemarks(TicketResolutionTeam.CUSTOMERCARE, TicketConstants.ESCALATE_TO_CUSTOMERCARE_REMARKS);
			} else {
				_LOGGER.info(String.format("Invalid ticketAction : %s ", action));
				continue;
			}
			try {
				if (ticketAction.isApplicable(ticket, action, defaultActionDetails)) {
					if (ticketAction.apply(ticket, action, defaultActionDetails)) {
						terminate = true;
						break;
					}
				}
			} catch (Exception e) {
				_LOGGER.error(String.format("Error while executing ticketAction : %s on ticket : %s ", action, ticketAction), e);
			}
		}
		if (!terminate) {
			executeDefaultAction(ticket, defaultActionDetails);
		}
	}

	private void executeDefaultAction(TicketEntity ticket, TicketActionDetailsBean actionDetailsBean) {
		String action = TicketConstants.ESCALATE_TO_CUSTOMERCARE_ACTION;
		TicketActionsInterface ticketAction = escalateToTeamAction;
		escalateToTeamAction.setTeamAndRemarks(TicketResolutionTeam.CUSTOMERCARE, TicketConstants.ESCALATE_TO_CUSTOMERCARE_REMARKS);
		try {
			if (ticketAction.isApplicable(ticket, action, actionDetailsBean)) {
				ticketAction.apply(ticket, action, TicketActionDetailsBean.newInstance());
			}
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while executing ticketAction : %s on ticket : %s ", action, ticketAction), e);
		}
	}

	public void populateTicketDetailsAsPerCategoryRoot(List<TicketEntity> tickets) {
		TicketRequestBean ticketRequestBean = ticketRequestUtils.getTicketRequest();
		String categoryRootLabel = tickets.get(0).getCategoryRoot().getLabel();
		String entityType = tickets.get(0).getRequesterEntityType();
		if (entityType.equals(EntityType.STORE.toString())) {
			if (categoryRootLabel.equals(TicketCategoryRoot.ORDER_ISSUE.toString())) {
				for (TicketEntity ticket : tickets) {
					// set ticket details from order item response
					OrderDetailsBean orderDetailsBean = ticket.getDetails().getOrderDetails();
					if (orderDetailsBean != null && !StringUtils.isEmpty(orderDetailsBean.getSkuCode())) {
						try {
							FranchiseOrderResponseBean orderResponseBean = ticketRequestBean.getOrderResponse();
							FranchiseOrderItemResponseBean orderItemResponseBean = ticketRequestBean.getOrderItemSkuMap().get(orderDetailsBean.getSkuCode());
							if (orderItemResponseBean == null) {
								throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND,
										String.format("Order Item can not be found with skuCode : %s and orderId : %s", orderDetailsBean.getSkuCode(),
												orderResponseBean.getId()), null));
							}
							orderDetailsBean.setWhId(orderItemResponseBean.getWhId());
							orderDetailsBean.setSkuCode(orderItemResponseBean.getSkuCode());
							orderDetailsBean.setOrderId(orderItemResponseBean.getOrderId());
							orderDetailsBean.setDeliveryDate(orderResponseBean.getDeliveryDate());
							orderDetailsBean.setDeliverySlot(orderDetailsBean.getDeliverySlot());
							orderDetailsBean.setUom(orderItemResponseBean.getUom());
							orderDetailsBean.setOrderedQty(orderItemResponseBean.getOrderedQty());
							orderDetailsBean.setDeliveredQty(orderItemResponseBean.getFinalQuantity());

							WhSkuResponse whSkuResponse = ticketRequestBean.getWhSkuResponseMap().get(orderDetailsBean.getSkuCode());
							if (whSkuResponse == null) {
								throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND,
										String.format("WH Sku Details not found with skuCode : %s, whId : %d and orderId : %s", orderDetailsBean.getSkuCode(),
												orderDetailsBean.getWhId(), orderDetailsBean.getOrderId()), null));
							}
							if (whSkuResponse.getPermissibleRefundQuantity() == null) {
								orderDetailsBean.setRefundableQty(0d);
							} else {
								orderDetailsBean.setRefundableQty(
										BigDecimal.valueOf(whSkuResponse.getPermissibleRefundQuantity()).divide(BigDecimal.valueOf(100d))
												.multiply(BigDecimal.valueOf(orderDetailsBean.getDeliveredQty()))
												.multiply(ticketRequestBean.getStoreCategoryRefundPermissibilityFactor()).doubleValue());
							}

							StoreReturnItemData storeReturnItemResponse = ticketRequestBean.getStoreReturnItemSkuMap().get(orderDetailsBean.getSkuCode());
							orderDetailsBean.setReturnQty(storeReturnItemResponse != null ? storeReturnItemResponse.getQuantity() : null);
							orderDetailsBean.setReturnRemarks(storeReturnItemResponse != null ? storeReturnItemResponse.getRemarks() : null);
							orderDetailsBean.setResolvedQty(null);
							ticket.getDetails().setOrderDetails(orderDetailsBean);

						} catch (Exception e) {
							_LOGGER.error(
									String.format("Error in updating ticket details for ticket with orderId : %s and skuCode : %s ", ticket.getReferenceId(),
											ticket.getDetails().getOrderDetails().getSkuCode()), e);
						}
					}
				}
			} else if (categoryRootLabel.equals(TicketCategoryRoot.PAYMENT_ISSUE.toString())) {
				for (TicketEntity ticket : tickets) {
					PaymentDetailsBean paymentDetailsBean = ticket.getDetails().getPaymentDetails();
					if (paymentDetailsBean != null && !StringUtils.isEmpty(paymentDetailsBean.getTxnDetail())) {
						try {
							paymentDetailsBean.setWalletStatementBeans(ticketRequestBean.getWalletStatementBeans());
						} catch (Exception e) {
							_LOGGER.error(String.format("Error in updating ticket details for ticket with txnDetail : %s ",
									ticket.getDetails().getPaymentDetails().getTxnDetail()), e);
						}
					}
				}
			}
		}
	}
}
