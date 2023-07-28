package com.sorted.rest.services.ticket.utils;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.ticket.actions.*;
import com.sorted.rest.services.ticket.beans.*;
import com.sorted.rest.services.ticket.constants.TicketConstants.*;
import com.sorted.rest.services.ticket.entity.TicketEntity;
import com.sorted.rest.services.ticket.entity.TicketItemEntity;
import com.sorted.rest.services.ticket.services.TicketHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Component
public class TicketActionUtils {

	static AppLogger _LOGGER = LoggingManager.getLogger(TicketActionUtils.class);

	@Autowired
	private TicketHistoryService ticketHistoryService;

	@Autowired
	private UserUtils userUtils;

	@Autowired
	private TicketRequestUtils ticketRequestUtils;

	@Autowired
	private OnlyAddRemarksAction onlyAddRemarksAction;

	@Autowired
	private AutomaticOrderRefundAction automaticOrderRefundAction;

	@Autowired
	private ProcessOrderRefundAction processOrderRefundAction;

	@Autowired
	private CloseTicketAction closeTicketAction;

	@Autowired
	private CancelTicketAction cancelTicketAction;

	@Autowired
	private AutomaticFullOrderRefundAction automaticFullOrderRefundAction;

	@Autowired
	private ProcessFullOrderRefundAction processFullOrderRefundAction;

	@Autowired
	private ChangeIssueCategoryAction changeIssueCategoryAction;

	@Autowired
	private BaseMapper<?, ?> mapper;

	public void invokeTicketCreateAction(TicketItemEntity item, Long ticketId) {
		TicketActionDetailsBean actionDetailsBean = TicketActionDetailsBean.newInstance();
		actionDetailsBean.setUserDetail(setRequesterDetails());
		if (item.getStatus().equals(TicketStatus.IN_PROGRESS)) {
			item.setRemarks(TicketCreateActions.NEW_TICKET_CREATED.getRemarks());
			actionDetailsBean.setAttachments(item.getAttachments());
			actionDetailsBean.setRemarks(TicketCreateActions.NEW_TICKET_CREATED.getRemarks());
			ticketHistoryService.addTicketHistory(ticketId, item.getId(), TicketCreateActions.NEW_TICKET_CREATED.toString(), actionDetailsBean);
		} else if (item.getStatus().equals(TicketStatus.DRAFT)) {
			item.setRemarks(TicketCreateActions.DRAFT_TICKET_CREATED.getRemarks());
			actionDetailsBean.setAttachments(item.getAttachments());
			actionDetailsBean.setRemarks(TicketCreateActions.DRAFT_TICKET_CREATED.getRemarks());
			ticketHistoryService.addTicketHistory(ticketId, item.getId(), TicketCreateActions.DRAFT_TICKET_CREATED.toString(), actionDetailsBean);
		}
	}

	public void invokeDraftTicketUpdateAction(TicketItemEntity item, Long ticketId) {
		item.setRemarks(TicketUpdateActions.DRAFT_TICKET_UPDATED.getRemarks());
		TicketActionDetailsBean actionDetailsBean = TicketActionDetailsBean.newInstance();
		actionDetailsBean.setUserDetail(setRequesterDetails());
		actionDetailsBean.setAttachments(item.getNewAttachments());
		actionDetailsBean.setRemarks(TicketUpdateActions.DRAFT_TICKET_UPDATED.getRemarks());
		ticketHistoryService.addTicketHistory(ticketId, item.getId(), TicketUpdateActions.DRAFT_TICKET_UPDATED.toString(), actionDetailsBean);
	}

	public void invokeTicketRaiseAction(TicketItemEntity item, TicketEntity ticket) {
		List<String> actions = item.getCategoryLeaf().getOnCreateActions();
		Boolean terminate = false;
		TicketActionDetailsBean actionDetailsBean = TicketActionDetailsBean.newInstance();
		actionDetailsBean.setUserDetail(setRequesterDetails());
		for (String action : actions) {
			TicketActionsInterface ticketAction = null;
			if (action.equals(TicketCreateActions.AUTOMATIC_ORDER_REFUND.toString())) {
				ticketAction = automaticOrderRefundAction;
				automaticOrderRefundAction.setTeamAndRemarks(TicketResolutionTeam.CUSTOMERCARE.toString(),
						TicketCreateActions.AUTOMATIC_ORDER_REFUND.getRemarks());
			} else if (action.equals(TicketCreateActions.AUTOMATIC_FULL_ORDER_REFUND.toString())) {
				ticketAction = automaticFullOrderRefundAction;
				automaticFullOrderRefundAction.setTeamAndRemarks(TicketResolutionTeam.CUSTOMERCARE.toString(),
						TicketCreateActions.AUTOMATIC_FULL_ORDER_REFUND.getRemarks());
				//			todo: tickets escalation not allowed in V1, add in subsequent releases
				//			} else if (action.equals(TicketCreateActions.ESCALATE_TO_WAREHOUSE.toString())) {
				//				ticketAction = escalateToTeamAction;
				//				escalateToTeamAction.setTeamAndRemarks(TicketResolutionTeam.WAREHOUSE.toString(), TicketCreateActions.ESCALATE_TO_WAREHOUSE.getRemarks());
				//			} else if (action.equals(TicketCreateActions.ESCALATE_TO_CUSTOMERCARE.toString())) {
				//				ticketAction = escalateToTeamAction;
				//				escalateToTeamAction.setTeamAndRemarks(TicketResolutionTeam.CUSTOMERCARE.toString(), TicketCreateActions.ESCALATE_TO_CUSTOMERCARE.getRemarks());
			} else {
				_LOGGER.info(String.format("Invalid ticketAction : %s ", action));
				continue;
			}
			if (ticketAction.isApplicable(item, ticket, action, actionDetailsBean)) {
				if (ticketAction.apply(item, ticket, action, actionDetailsBean)) {
					terminate = true;
					break;
				}
			}
		}
		//		todo: tickets escalation not allowed in V1, add in subsequent releases
		//		if (!terminate) {
		//			executeDefaultAction(item, ticket, actionDetailsBean);
		//		}
		//	}

		//	private void executeDefaultAction(TicketItemEntity item, TicketEntity ticket, TicketActionDetailsBean actionDetailsBean) {
		//		String action = TicketCreateActions.ESCALATE_TO_CUSTOMERCARE.toString();
		//		TicketActionsInterface ticketAction = escalateToTeamAction;
		//		escalateToTeamAction.setTeamAndRemarks(TicketResolutionTeam.CUSTOMERCARE.toString(), TicketCreateActions.ESCALATE_TO_CUSTOMERCARE.getRemarks());
		//		if (ticketAction.isApplicable(item, ticket, action, actionDetailsBean)) {
		//			ticketAction.apply(item, ticket, action, TicketActionDetailsBean.newInstance());
		//		}
	}

	public void populateTicketResolutionAsPerCategoryRoot(TicketEntity requestTicket, List<TicketItemEntity> requestTicketItems) {
		TicketRequestBean ticketRequestBean = ticketRequestUtils.getTicketRequest();
		String categoryRootLabel = requestTicket.getCategoryRoot().getLabel();
		String entityType = requestTicket.getRequesterEntityType();
		if (entityType.equals(EntityType.STORE.toString())) {
			TicketMetadataBean ticketMetadata = requestTicket.getMetadata();
			if (requestTicket.getMetadata().getStoreDetails().getStoreId() == null) {
				ticketMetadata.setStoreDetails(mapper.mapSrcToDest(ticketRequestBean.getStoreDataResponse(), requestTicket.getMetadata().getStoreDetails()));
				requestTicket.setMetadata(ticketMetadata);
			}
			if (categoryRootLabel.equals(TicketCategoryRoot.ORDER_ISSUE.toString())) {
				FranchiseOrderResponseBean orderResponseBean = ticketRequestBean.getOrderResponse();

				if (requestTicket.getMetadata().getOrderDetails() == null) {
					OrderDetailsBean orderDetailsBean = OrderDetailsBean.newInstance();
					orderDetailsBean.setOrderId(orderResponseBean.getId());
					orderDetailsBean.setDisplayOrderId(orderResponseBean.getDisplayOrderId());
					orderDetailsBean.setOrderStatus(orderResponseBean.getStatus().toString()); // status updated On Get
					orderDetailsBean.setFinalOrderBillAmount(orderResponseBean.getFinalBillAmount());
					orderDetailsBean.setChallanUrl(orderResponseBean.getChallanUrl());
					orderDetailsBean.setDeliveryDate(orderResponseBean.getDeliveryDate());
					orderDetailsBean.setDeliverySlot(orderResponseBean.getSlot());
					orderDetailsBean.setTotalRefundableAmount(0d);
					orderDetailsBean.setTotalRefundAmount(0d);
					ticketMetadata.setOrderDetails(orderDetailsBean);
					requestTicket.setMetadata(ticketMetadata);
				}

				if (requestTicket.getHasNew()) {
					for (TicketItemEntity item : requestTicketItems) {

						OrderItemDetailsBean orderItemDetailsBean = item.getDetails().getOrderDetails();
						if (orderItemDetailsBean != null && !StringUtils.isEmpty(orderItemDetailsBean.getSkuCode())) {
							FranchiseOrderItemResponseBean orderItemResponseBean = ticketRequestBean.getOrderItemSkuMap()
									.get(orderItemDetailsBean.getSkuCode());
							if (orderItemResponseBean == null) {
								throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND,
										String.format("Order Item can not be found with skuCode : %s and orderId : %s", orderItemDetailsBean.getSkuCode(),
												orderResponseBean.getId()), null));
							}

							orderItemDetailsBean.setOrderId(UUID.fromString(requestTicket.getReferenceId()));
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
										String.format("WH Sku Details not found with skuCode : %s, whId : %d and orderId : %s",
												orderItemDetailsBean.getSkuCode(), orderItemDetailsBean.getWhId(), orderItemDetailsBean.getOrderId()), null));
							}
							if (whSkuResponse.getPermissibleRefundQuantity() == null) {
								orderItemDetailsBean.setRefundableQty(0d);
							} else {
								orderItemDetailsBean.setRefundableQty(
										BigDecimal.valueOf(whSkuResponse.getPermissibleRefundQuantity()).divide(BigDecimal.valueOf(100d))
												.multiply(BigDecimal.valueOf(orderItemDetailsBean.getDeliveredQty()))
												.multiply(requestTicket.getMetadata().getStoreDetails().getRefundPermissibilityFactor())
												.setScale(4, RoundingMode.HALF_UP).doubleValue());
							}
							orderItemDetailsBean.setResolvedQty(null);
							item.getDetails().setOrderDetails(orderItemDetailsBean);

						}
					}
				}
				for (TicketItemEntity item : requestTicketItems) {
					if (item.getDetails().getOrderDetails() != null && item.getDetails().getOrderDetails().getSkuCode() != null) {
						invokeUpdateStoreReturnInfoAction(item, requestTicket.getId(),
								ticketRequestBean.getStoreReturnItemSkuMap().get(item.getDetails().getOrderDetails().getSkuCode()));
					}
				}
				//			todo: tickets for PAYMENT_ISSUE with referenceId not allowed in V1, add in subsequent releases
				//			} else if (categoryRootLabel.equals(TicketCategoryRoot.PAYMENT_ISSUE.toString())) {
				//				for (TicketItemEntity item : ticket.getItems()) {
				//					PaymentDetailsBean paymentDetailsBean = mapper.mapSrcToDest(ticketRequestBean.getWalletStatementBean(), PaymentDetailsBean.newInstance());
				//					item.getResolutionDetails().setPaymentDetails(paymentDetailsBean);
				//				}
			}
			requestTicket.setMetadata(ticketMetadata);
		}
	}

	public void invokeTicketUpdateAction(TicketItemEntity item, TicketEntity ticket, UpdateTicketBean updateTicketBean) {
		String action = updateTicketBean.getAction();
		TicketActionDetailsBean actionDetailsBean = TicketActionDetailsBean.newInstance();
		actionDetailsBean.setUserDetail(setRequesterDetails());

		TicketActionsInterface ticketAction = null;
		if (action.equals(TicketUpdateActions.ONLY_ADD_REMARKS.toString())) {
			ticketAction = onlyAddRemarksAction;
			onlyAddRemarksAction.setAttachments(updateTicketBean.getAttachments());
			onlyAddRemarksAction.setRemarks(updateTicketBean.getRemarks());
		} else if (action.equals(TicketUpdateActions.CHANGE_ISSUE_CATEGORY.toString())) {
			ticketAction = changeIssueCategoryAction;
			changeIssueCategoryAction.setAttachments(updateTicketBean.getAttachments());
			changeIssueCategoryAction.setCategoryLeafId(updateTicketBean.getCategoryLeafId());
			changeIssueCategoryAction.setRemarks(updateTicketBean.getRemarks());
		} else if (action.equals(TicketUpdateActions.PROCESS_ORDER_REFUND.toString())) {
			ticketAction = processOrderRefundAction;
			processOrderRefundAction.setAttachments(updateTicketBean.getAttachments());
			processOrderRefundAction.setResolvedQuantity(updateTicketBean.getResolvedQuantity());
			processOrderRefundAction.setRemarks(updateTicketBean.getRemarks());
		} else if (action.equals(TicketUpdateActions.CLOSE_WITH_REMARKS.toString())) {
			ticketAction = closeTicketAction;
			closeTicketAction.setAttachments(updateTicketBean.getAttachments());
			closeTicketAction.setRemarks(updateTicketBean.getRemarks());
		} else if (action.equals(TicketUpdateActions.CANCEL_WITH_REMARKS.toString())) {
			ticketAction = cancelTicketAction;
			cancelTicketAction.setAttachments(updateTicketBean.getAttachments());
			cancelTicketAction.setRemarks(updateTicketBean.getRemarks());
		} else if (action.equals(TicketUpdateActions.PROCESS_FULL_ORDER_REFUND.toString())) {
			ticketAction = processFullOrderRefundAction;
			processFullOrderRefundAction.setAttachments(updateTicketBean.getAttachments());
			processFullOrderRefundAction.setRemarks(updateTicketBean.getRemarks());
		} else {
			_LOGGER.info(String.format("Invalid ticketAction : %s ", action));
		}

		if (ticketAction != null && ticketAction.isApplicable(item, ticket, action, actionDetailsBean)) {
			ticketAction.apply(item, ticket, action, actionDetailsBean);
		} else {
			throw new ValidationException(
					ErrorBean.withError(Errors.INVALID_REQUEST, String.format("Ticket can not be updated with action : %s", action), null));
		}
	}

	public void addParentTicketHistory(TicketEntity ticket, Boolean hasNew, Integer draftCountOld, Integer pendingCountOld, Integer closedCountOld,
			Integer cancelledCountOld) {
		TicketActionDetailsBean actionDetailsBean = TicketActionDetailsBean.newInstance();
		actionDetailsBean.setUserDetail(setRequesterDetails());
		if (draftCountOld != null && draftCountOld > 0 && ticket.getDraftCount() == 0) {
			actionDetailsBean.setRemarks(ParentTicketUpdateActions.ALL_DRAFT_CHILDREN_MOVED.getRemarks());
			ticketHistoryService.addTicketHistory(ticket.getId(), null, ParentTicketUpdateActions.ALL_DRAFT_CHILDREN_MOVED.toString(), actionDetailsBean);
		} else if (pendingCountOld != null && pendingCountOld > 0 && ticket.getPendingCount() == 0 && ticket.getDraftCount() == 0) {
			actionDetailsBean.setRemarks(ParentTicketUpdateActions.ALL_PENDING_CHILDREN_MOVED.getRemarks());
			ticketHistoryService.addTicketHistory(ticket.getId(), null, ParentTicketUpdateActions.ALL_PENDING_CHILDREN_MOVED.toString(), actionDetailsBean);
		}
	}

	private UserDetail setRequesterDetails() {
		return ticketRequestUtils.getTicketRequest().getRequesterUserDetail() != null ?
				ticketRequestUtils.getTicketRequest().getRequesterUserDetail() :
				userUtils.getUserDetail(SessionUtils.getAuthUserId());
	}

	public void invokeUpdateStoreReturnInfoAction(TicketItemEntity item, Long ticketId, StoreReturnItemData storeReturnItemResponse) {
		if (storeReturnItemResponse == null) {
			return;
		}
		boolean updated = false;
		if (storeReturnItemResponse.getQuantity() != null && !storeReturnItemResponse.getQuantity()
				.equals(item.getDetails().getOrderDetails().getReturnQty())) {
			updated = true;
			item.getDetails().getOrderDetails().setReturnQty(storeReturnItemResponse.getQuantity());
		}

		if (storeReturnItemResponse.getRemarks() != null && !storeReturnItemResponse.getRemarks()
				.equals(item.getDetails().getOrderDetails().getReturnRemarks())) {
			if (!updated)
				updated = true;
			item.getDetails().getOrderDetails().setReturnRemarks(storeReturnItemResponse.getRemarks());
		}

		if (storeReturnItemResponse.getQaResult() != null && !storeReturnItemResponse.getQaResult()
				.equals(item.getDetails().getOrderDetails().getReturnQaResult())) {
			if (!updated)
				updated = true;
			item.getDetails().getOrderDetails().setReturnQaResult(storeReturnItemResponse.getQaResult());
		}

		if (storeReturnItemResponse.getRefundQty() != null && !storeReturnItemResponse.getRefundQty()
				.equals(item.getDetails().getOrderDetails().getReturnRefundQty())) {
			if (!updated)
				updated = true;
			item.getDetails().getOrderDetails().setReturnRefundQty(storeReturnItemResponse.getRefundQty());
		}

		if (updated) {
			TicketActionDetailsBean actionDetailsBean = TicketActionDetailsBean.newInstance();
			actionDetailsBean.setUserDetail(setRequesterDetails());
			actionDetailsBean.setRemarks(TicketUpdateActions.STORE_RETURN_DATA_UPDATED.getRemarks());
			ticketHistoryService.addTicketHistory(ticketId, item.getId(), TicketUpdateActions.STORE_RETURN_DATA_UPDATED.toString(), actionDetailsBean);
		}
	}
}
