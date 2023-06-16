package com.sorted.rest.services.ticket.constants;

import java.math.BigDecimal;

public class TicketConstants {

	public static final String TICKETS_TABLE_NAME = "tickets";

	public static final String TICKET_HISTORY_TABLE_NAME = "ticket_histories";

	public static final String TICKET_CATEGORY_TABLE_NAME = "ticket_categories";

	public static final Integer TICKET_RAISING_USER_USERTYPE = 2;

	public static final String CUSTOMER_CARE_ROLE = "CUSTOMERCARE";

	public static final String ESCALATE_TO_CUSTOMERCARE_ACTION = "ESCALATE_TO_CUSTOMERCARE";

	public static final String AUTOMATIC_ORDER_REFUND_ACTION = "AUTOMATIC_ORDER_REFUND";

	public static final String ESCALATE_TO_WAREHOUSE_ACTION = "ESCALATE_TO_WAREHOUSE";

	public static final String ESCALATE_TO_CUSTOMERCARE_REMARKS = "Ticket escalated to customer care executive";

	public static final String AUTOMATIC_ORDER_REFUND_REMARKS = "Automatic refund initiated";

	public static final String ESCALATE_TO_WAREHOUSE_REMARKS = "Ticket escalated to warehouse executive";

	public static final String NEW_TICKET_CREATED_REMARKS = "New Ticket Created";

	public static final String NEW_TICKET_CREATED_ACTION = "New Ticket Created";

	public static final BigDecimal DEFAULT_STORE_CATEGORY_REFUND_PERMISSIBILITY_FACTOR = BigDecimal.ONE;

	public enum TicketStatus {

		DRAFT, IN_PROGRESS, CLOSED
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
		ORDER_ISSUE, POS_ISSUE, PAYMENT_ISSUE, UNDELIVERED_ISSUE, PRICING_ISSUE, APP_ISSUE, SUGGESTION
	}

	public enum TicketPlatform {

		PARTNER_APP, MIDDLE_MILE_APP, BACKOFFICE
	}
}
