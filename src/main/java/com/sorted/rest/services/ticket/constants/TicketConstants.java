package com.sorted.rest.services.ticket.constants;

import java.math.BigDecimal;

public class TicketConstants {

	public static final String TICKETS_TABLE_NAME = "tickets";

	public static final String TICKET_ITEMS_TABLE_NAME = "ticket_items";

	public static final String TICKET_HISTORY_TABLE_NAME = "ticket_histories";

	public static final String TICKET_CATEGORY_TABLE_NAME = "ticket_categories";

	public static final Integer TICKET_RAISING_USER_USERTYPE = 2;

	public static final String CLOSED_TICKET_ASSIGNED_TEAM = "NA";

	public static final String INTERNAL_USER_NAME = "Internal User";

	public static final String STORE_RETURN_TICKET_DESCRIPTION = "Ticket created via store return";

	public static final BigDecimal DEFAULT_STORE_CATEGORY_REFUND_PERMISSIBILITY_FACTOR = BigDecimal.ONE;

	public enum TicketStatus {

		DRAFT, IN_PROGRESS, CLOSED//, CANCELLED
	}

	public enum EntityType {

		USER(1), STORE(2);

		private final int value;

		EntityType(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		public static EntityType fromString(String val) {
			for (EntityType value : EntityType.values()) {
				if (value.toString().equals(val))
					return value;
			}
			return null;
		}
	}

	public enum TicketResolutionTeam {

		CUSTOMERCARE(1), WAREHOUSE(2);

		private final int value;

		TicketResolutionTeam(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum FranchiseOrderStatus {

		IN_CART(0), NEW_ORDER(1), ORDER_BILLED(2), OUT_FOR_DELIVERY(3), ORDER_DELIVERED(4), REFUND_REQUESTED(5), ORDER_REFUNDED(6), CANCELLED(7), FAILED(8);

		private int value;

		private FranchiseOrderStatus(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum FranchiseOrderItemStatus {
		PENDING, PACKED, NOT_AVAILABLE;
	}

	public enum StoreReturnStatus {

		ISSUED(0), DRAFT(1), RECEIVED(2), CLOSED(3);

		private int value;

		private StoreReturnStatus(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum TicketCategoryRoot {
		ORDER_ISSUE, POS_ISSUE, PAYMENT_ISSUE, PRICING_ISSUE, APP_ISSUE
	}

	public enum TicketPlatform {

		PARTNER_APP, MIDDLE_MILE_APP, IMS, STORE_RETURN
	}

	public enum TicketCreateActions {

		NEW_TICKET_CREATED("New Ticket Created"), DRAFT_TICKET_CREATED("Draft Ticket Created"), ESCALATE_TO_CUSTOMERCARE(
				"Ticket escalated to customer care executive"), ESCALATE_TO_WAREHOUSE("Ticket escalated to warehouse executive"), AUTOMATIC_ORDER_REFUND(
				"Automatic refund initiated");

		private String remarks;

		private TicketCreateActions(String remarks) {
			this.remarks = remarks;
		}

		public String getRemarks() {
			return remarks;
		}
	}

	public enum TicketUpdateActions {

		DRAFT_TICKET_UPDATED("Draft Ticket Updated"), STORE_RETURN_DATA_UPDATED("Store Return Data Updated"), PROCESS_ORDER_REFUND(
				"Ticket closed after generating refund for the order for quantity %s against issue raised for %s %s"), CLOSE_WITH_REMARKS(
				"Ticket closed with remarks %s"), CANCEL_WITH_REMARKS("Ticket cancelled with remarks %s");

		private String remarks;

		private TicketUpdateActions(String remarks) {
			this.remarks = remarks;
		}

		public String getRemarks() {
			return remarks;
		}
	}

	public enum ParentTicketUpdateActions {

		NEW_PARENT_CREATED("New parent ticket created"), NEW_TICKET_ADDED("New ticket(s) added"), NEW_DRAFT_TICKET_ADDED(
				"New ticket(s) added as Draft"), ALL_DRAFT_TICKET_MOVED("All draft ticket(s) moved"), ALL_TICKET_CLOSED("All ticket(s) closed");

		private String remarks;

		private ParentTicketUpdateActions(String remarks) {
			this.remarks = remarks;
		}

		public String getRemarks() {
			return remarks;
		}
	}
}
