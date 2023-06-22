package com.sorted.rest.services.ticket.utils;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.ticket.actions.AutomaticOrderRefundAction;
import com.sorted.rest.services.ticket.actions.EscalateToTeamAction;
import com.sorted.rest.services.ticket.actions.TicketActionsInterface;
import com.sorted.rest.services.ticket.beans.*;
import com.sorted.rest.services.ticket.constants.TicketConstants.*;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
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

	@Autowired
	private BaseMapper<?, ?> mapper;

	public void invokeTicketCreateAction(TicketItemEntity item, Long ticketId) {
		TicketActionDetailsBean actionDetailsBean = TicketActionDetailsBean.newInstance();
		actionDetailsBean.setUserDetail(ticketRequestUtils.getTicketRequest().getRequesterUserDetail());
		actionDetailsBean.setRemarks(TicketCreateActions.NEW_TICKET_CREATED.getRemarks());
		ticketHistoryService.addTicketHistory(ticketId, item.getId(), TicketCreateActions.NEW_TICKET_CREATED.toString(), actionDetailsBean);
	}

	public void invokeDraftTicketUpdateAction(TicketItemEntity item, Long ticketId) {
		item.setRemarks(TicketUpdateActions.DRAFT_TICKET_UPDATED.getRemarks());
		TicketActionDetailsBean actionDetailsBean = TicketActionDetailsBean.newInstance();
		actionDetailsBean.setUserDetail(ticketRequestUtils.getTicketRequest().getRequesterUserDetail());
		actionDetailsBean.setRemarks(TicketUpdateActions.DRAFT_TICKET_UPDATED.getRemarks());
		ticketHistoryService.addTicketHistory(ticketId, item.getId(), TicketUpdateActions.DRAFT_TICKET_UPDATED.toString(), actionDetailsBean);
	}

	public void invokeTicketRaiseAction(TicketItemEntity item, Long ticketId) {
		List<String> actions = item.getCategoryLeaf().getOnCreateActions();
		Boolean terminate = false;
		TicketActionDetailsBean actionDetailsBean = TicketActionDetailsBean.newInstance();
		actionDetailsBean.setUserDetail(ticketRequestUtils.getTicketRequest().getInternalUserDetail());
		for (String action : actions) {
			TicketActionsInterface ticketAction = null;
			if (action.equals(TicketCreateActions.AUTOMATIC_ORDER_REFUND.toString())) {
				ticketAction = automaticOrderRefundAction;
				automaticOrderRefundAction.setTeamAndRemarks(TicketResolutionTeam.CUSTOMERCARE.toString(),
						TicketCreateActions.AUTOMATIC_ORDER_REFUND.getRemarks());
			} else if (action.equals(TicketCreateActions.ESCALATE_TO_WAREHOUSE.toString())) {
				ticketAction = escalateToTeamAction;
				escalateToTeamAction.setTeamAndRemarks(TicketResolutionTeam.WAREHOUSE.toString(), TicketCreateActions.ESCALATE_TO_WAREHOUSE.getRemarks());
			} else if (action.equals(TicketCreateActions.ESCALATE_TO_CUSTOMERCARE.toString())) {
				ticketAction = escalateToTeamAction;
				escalateToTeamAction.setTeamAndRemarks(TicketResolutionTeam.CUSTOMERCARE.toString(), TicketCreateActions.ESCALATE_TO_CUSTOMERCARE.getRemarks());
			} else {
				_LOGGER.info(String.format("Invalid ticketAction : %s ", action));
			}
			if (ticketAction.isApplicable(item, ticketId, action, actionDetailsBean)) {
				if (ticketAction.apply(item, ticketId, action, actionDetailsBean)) {
					terminate = true;
					break;
				}
			}
		}
		if (!terminate) {
			executeDefaultAction(item, ticketId, actionDetailsBean);
		}
	}

	private void executeDefaultAction(TicketItemEntity item, Long ticketId, TicketActionDetailsBean actionDetailsBean) {
		String action = TicketCreateActions.ESCALATE_TO_CUSTOMERCARE.toString();
		TicketActionsInterface ticketAction = escalateToTeamAction;
		escalateToTeamAction.setTeamAndRemarks(TicketResolutionTeam.CUSTOMERCARE.toString(), TicketCreateActions.ESCALATE_TO_CUSTOMERCARE.getRemarks());
		if (ticketAction.isApplicable(item, ticketId, action, actionDetailsBean)) {
			ticketAction.apply(item, ticketId, action, TicketActionDetailsBean.newInstance());
		}
	}

	public void populateTicketResolutionAsPerCategoryRoot(TicketEntity ticket) {
		TicketRequestBean ticketRequestBean = ticketRequestUtils.getTicketRequest();
		String categoryRootLabel = ticket.getCategoryRoot().getLabel();
		String entityType = ticket.getRequesterEntityType();
		if (entityType.equals(EntityType.STORE.toString())) {
			TicketMetadataBean ticketMetadata = ticket.getMetadata();
			ticketMetadata.setStoreDetails(mapper.mapSrcToDest(ticketRequestBean.getStoreDataResponse(), StoreDetailsBean.newInstance()));
			if (categoryRootLabel.equals(TicketCategoryRoot.ORDER_ISSUE.toString())) {
				FranchiseOrderResponseBean orderResponseBean = ticketRequestBean.getOrderResponse();

				OrderDetailsBean orderDetailsBean = OrderDetailsBean.newInstance();
				orderDetailsBean.setOrderId(orderResponseBean.getId());
				orderDetailsBean.setDisplayOrderId(orderResponseBean.getDisplayOrderId());
				orderDetailsBean.setOrderStatus(orderResponseBean.getStatus().toString());
				orderDetailsBean.setFinalOrderBillAmount(orderResponseBean.getFinalBillAmount());
				orderDetailsBean.setChallanUrl(orderResponseBean.getChallanUrl());
				orderDetailsBean.setDeliveryDate(orderResponseBean.getDeliveryDate());
				orderDetailsBean.setDeliverySlot(orderResponseBean.getSlot());
				ticketMetadata.setOrderDetails(orderDetailsBean);

				for (TicketItemEntity item : ticket.getItems()) {
					item.setResolutionDetails(mapper.mapSrcToDest(item.getDetails(), ResolutionDetailsBean.newInstance()));

					OrderItemDetailsBean orderItemDetailsBean = item.getResolutionDetails().getOrderDetails();
					if (orderItemDetailsBean != null && !StringUtils.isEmpty(orderItemDetailsBean.getSkuCode())) {
						FranchiseOrderItemResponseBean orderItemResponseBean = ticketRequestBean.getOrderItemSkuMap().get(orderItemDetailsBean.getSkuCode());
						if (orderItemResponseBean == null) {
							throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND,
									String.format("Order Item can not be found with skuCode : %s and orderId : %s", orderItemDetailsBean.getSkuCode(),
											orderResponseBean.getId()), null));
						}

						orderItemDetailsBean.setOrderId(orderResponseBean.getId());
						orderItemDetailsBean.setWhId(orderItemResponseBean.getWhId());
						orderItemDetailsBean.setSkuCode(orderItemResponseBean.getSkuCode());
						orderItemDetailsBean.setProductName(orderItemResponseBean.getProductName());
						orderItemDetailsBean.setFinalItemAmount(orderItemResponseBean.getFinalAmount());
						orderItemDetailsBean.setItemStatus(orderItemResponseBean.getStatus().toString());
						orderItemDetailsBean.setProrataAmount(orderItemResponseBean.getProrataAmount());
						orderItemDetailsBean.setUom(orderItemResponseBean.getUom());
						orderItemDetailsBean.setOrderedQty(orderItemResponseBean.getOrderedQty());
						orderItemDetailsBean.setDeliveredQty(orderItemResponseBean.getFinalQuantity());

						WhSkuResponse whSkuResponse = ticketRequestBean.getWhSkuResponseMap().get(orderItemDetailsBean.getSkuCode());
						if (whSkuResponse == null) {
							throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND,
									String.format("WH Sku Details not found with skuCode : %s, whId : %d and orderId : %s", orderItemDetailsBean.getSkuCode(),
											orderItemDetailsBean.getWhId(), orderItemDetailsBean.getOrderId()), null));
						}
						if (whSkuResponse.getPermissibleRefundQuantity() == null) {
							orderItemDetailsBean.setRefundableQty(0d);
						} else {
							orderItemDetailsBean.setRefundableQty(
									BigDecimal.valueOf(whSkuResponse.getPermissibleRefundQuantity()).divide(BigDecimal.valueOf(100d))
											.multiply(BigDecimal.valueOf(orderItemDetailsBean.getDeliveredQty()))
											.multiply(ticketRequestBean.getStoreCategoryRefundPermissibilityFactor()).doubleValue());
						}

						StoreReturnItemData storeReturnItemResponse = ticketRequestBean.getStoreReturnItemSkuMap().get(orderItemDetailsBean.getSkuCode());
						orderItemDetailsBean.setReturnQty(storeReturnItemResponse != null ? storeReturnItemResponse.getQuantity() : null);
						orderItemDetailsBean.setReturnRemarks(storeReturnItemResponse != null ? storeReturnItemResponse.getRemarks() : null);
						orderItemDetailsBean.setReturnRefundSuggestion(storeReturnItemResponse != null ? storeReturnItemResponse.getRefundSuggestion() : null);
						orderItemDetailsBean.setReturnRefundQty(storeReturnItemResponse != null ? storeReturnItemResponse.getRefundQuantity() : null);
						orderItemDetailsBean.setResolvedQty(null);
						item.getResolutionDetails().setOrderDetails(orderItemDetailsBean);
					}
				}
				//			todo: tickets for PAYMENT_ISSUE with referenceId not allowed in V1, add in subsequent releases
				//			} else if (categoryRootLabel.equals(TicketCategoryRoot.PAYMENT_ISSUE.toString())) {
				//				for (TicketItemEntity item : ticket.getItems()) {
				//					PaymentDetailsBean paymentDetailsBean = mapper.mapSrcToDest(ticketRequestBean.getWalletStatementBean(), PaymentDetailsBean.newInstance());
				//					item.getResolutionDetails().setPaymentDetails(paymentDetailsBean);
				//				}
			}
			ticket.setMetadata(ticketMetadata);
		}
	}

	public void invokeTicketUpdateAction(TicketItemEntity item, Long ticketId, UpdateTicketBean updateTicketBean) {
		String action = updateTicketBean.getAction();
		Boolean terminate = false;
		TicketActionDetailsBean actionDetailsBean = TicketActionDetailsBean.newInstance();
		actionDetailsBean.setUserDetail(ticketRequestUtils.getTicketRequest().getRequesterUserDetail());

		TicketActionsInterface ticketAction = null;
		if (action.equals(TicketUpdateActions.PROCESS_ORDER_REFUND.toString())) {
			ticketAction = automaticOrderRefundAction;
			automaticOrderRefundAction.setTeamAndRemarks(null,
					String.format(TicketUpdateActions.PROCESS_ORDER_REFUND.getRemarks(), updateTicketBean.getResolvedQuantity(),
							(item.getResolutionDetails().getOrderDetails() != null && item.getResolutionDetails().getOrderDetails().getIssueQty() != null) ?
									item.getResolutionDetails().getOrderDetails().getIssueQty() :
									"NA",
							(item.getResolutionDetails().getOrderDetails() != null && item.getResolutionDetails().getOrderDetails().getUom() != null) ?
									item.getResolutionDetails().getOrderDetails().getUom() :
									"NA"));
		} else {
			_LOGGER.info(String.format("Invalid ticketAction : %s ", action));
		}

		if (ticketAction.isApplicable(item, ticketId, action, actionDetailsBean)) {
			ticketAction.apply(item, ticketId, action, actionDetailsBean);
		}
	}

	public void addParentTicketHistory(TicketEntity ticket, Boolean hasNew, Integer hasDraft, Integer isClosed) {
		TicketActionDetailsBean actionDetailsBean = TicketActionDetailsBean.newInstance();
		actionDetailsBean.setUserDetail(ticketRequestUtils.getTicketRequest().getRequesterUserDetail());
		if ((hasDraft == null || hasDraft == 0) && ticket.getHasDraft() == 1) {
			actionDetailsBean.setRemarks(ParentTicketUpdateActions.NEW_DRAFT_TICKET_ADDED.getRemarks());
			ticketHistoryService.addTicketHistory(ticket.getId(), null, ParentTicketUpdateActions.NEW_DRAFT_TICKET_ADDED.toString(), actionDetailsBean);
		} else if (hasNew) {
			actionDetailsBean.setRemarks(ParentTicketUpdateActions.NEW_TICKET_ADDED.getRemarks());
			ticketHistoryService.addTicketHistory(ticket.getId(), null, ParentTicketUpdateActions.NEW_TICKET_ADDED.toString(), actionDetailsBean);
		} else if (hasDraft == 1 && ticket.getHasDraft() == 0) {
			actionDetailsBean.setRemarks(ParentTicketUpdateActions.DRAFT_TICKET_MOVED.getRemarks());
			ticketHistoryService.addTicketHistory(ticket.getId(), null, ParentTicketUpdateActions.DRAFT_TICKET_MOVED.toString(), actionDetailsBean);
		} else if (isClosed == 0 && ticket.getIsClosed() == 1) {
			actionDetailsBean.setRemarks(ParentTicketUpdateActions.ALL_TICKET_CLOSED.getRemarks());
			ticketHistoryService.addTicketHistory(ticket.getId(), null, ParentTicketUpdateActions.ALL_TICKET_CLOSED.toString(), actionDetailsBean);
		}
	}
}
