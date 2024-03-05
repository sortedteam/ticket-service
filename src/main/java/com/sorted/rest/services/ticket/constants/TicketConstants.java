package com.sorted.rest.services.ticket.constants;

import java.math.BigDecimal;

public class TicketConstants {

	public static final String TICKETS_TABLE_NAME = "tickets";

	public static final String TICKET_ITEMS_TABLE_NAME = "ticket_items";

	public static final String TICKET_HISTORY_TABLE_NAME = "ticket_histories";

	public static final String TICKET_CATEGORY_TABLE_NAME = "ticket_categories";

	public static final Integer TICKET_RAISING_USER_USERTYPE = 2;

	public static final String CLOSED_TICKET_ASSIGNED_TEAM = "NA";

	public static final String UPDATED_TICKET_DEFAULT_REMARKS = "NA";

	public static final String INTERNAL_USER_NAME = "Internal User";

	public static final String STORE_RETURN_TICKET_DESCRIPTION = "Ticket created via store return";

	public static final String STORE_RETURN_TICKET_CATEGORY_LEAF_LABEL = "QUALITY_ISSUE";

	public static final BigDecimal DEFAULT_STORE_CATEGORY_REFUND_PERMISSIBILITY_FACTOR = BigDecimal.ONE;

	public static final String CLIENT_KEY_FOR_REFUND = "REFUND_VIA_TICKET";

	public static final String CLIENT_KEY_FOR_CANCEL = "ORDER_CANCELLED_VIA_TICKET";

	public static final String FULL_ORDER_REFUND_PRODUCT_NAME = "Full Order";

	public static final String DEFAULT_USER_CATEGORY = "Good";

	public static final String DELIVERY_APP_AUTO_REFUND_REMARKS = "Refund has been given by delivery person";

	public static final String IMS_APP_AUTO_REFUND_REMARKS = "Refund has been given by CC team";

	public enum TicketStatus {

		DRAFT, IN_PROGRESS, CLOSED, CANCELLED
	}

	public enum EntityType {

		USER, STORE;
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

	public enum TicketCategoryRoot {
		ORDER_ISSUE, OTHER_ISSUES, CONSUMER_ORDER_ISSUE, CONSUMER_OTHER_ISSUES //other issues contains all non orders
	}

	public enum TicketPlatform {

		PARTNER_APP, MIDDLE_MILE_APP, IMS, STORE_RETURN, CONSUMER_APP, DELIVERY_APP
	}

	public enum TicketCreateActions {

		NEW_TICKET_CREATED("New Ticket Created"), DRAFT_TICKET_CREATED("Draft Ticket Created"), ESCALATE_TO_CUSTOMERCARE(
				"Ticket escalated to customer care executive"), ESCALATE_TO_WAREHOUSE("Ticket escalated to warehouse executive"), AUTOMATIC_ORDER_REFUND(
				"Automatic refund initiated"), AUTOMATIC_FULL_ORDER_REFUND("Automatic full order refund initiated"), AUTOMATIC_ORDER_CANCEL(
				"Automatic Order cancelled with remarks initiated"), AUTOMATIC_CONSUMER_ORDER_REFUND(
				"Automatic consumer order refund initiated"), AUTOMATIC_FULL_CONSUMER_ORDER_REFUND(
				"Automatic full consumer order refund initiated"), PROCESS_CONSUMER_ORDER_REFUND(
				"Successfully refunded with remarks %s"), PROCESS_FULL_CONSUMER_ORDER_REFUND("Full order  refunded with remarks %s");


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
				"Successfully refunded with remarks %s"), ONLY_ADD_REMARKS("%s"), CLOSE_WITH_REMARKS("%s"), CANCEL_WITH_REMARKS(
				"%s"), PROCESS_FULL_ORDER_REFUND("Full order  refunded with remarks %s"), CHANGE_ISSUE_CATEGORY(
				"Issue category updated with remarks %s"), PROCESS_CONSUMER_ORDER_REFUND(
				"Successfully refunded with remarks %s"), PROCESS_FULL_CONSUMER_ORDER_REFUND("Full order  refunded with remarks %s");

		private String remarks;

		private TicketUpdateActions(String remarks) {
			this.remarks = remarks;
		}

		public String getRemarks() {
			return remarks;
		}
	}

	public enum ParentTicketUpdateActions {

		ALL_DRAFT_CHILDREN_MOVED("All draft child ticket(s) moved"), ALL_PENDING_CHILDREN_MOVED("All pending child ticket(s) moved");

		private String remarks;

		private ParentTicketUpdateActions(String remarks) {
			this.remarks = remarks;
		}

		public String getRemarks() {
			return remarks;
		}
	}

	public enum UserRoles {

		CCEXECUTIVE, CCMANAGER
	}
}
