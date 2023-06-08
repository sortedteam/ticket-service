package com.sorted.rest.services.ticket.constants;

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
}
