package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class ImsConsumerOrderRefundBean implements Serializable {

	private static final long serialVersionUID = 4143679869385600722L;

	private UUID parentOrderId;

	private List<ImsConsumerOrderRefundItemBean> refundOrderItems;

	private Long ticketId;

	private Long ticketItemId;

	public static ImsConsumerOrderRefundBean newInstance() {
		return new ImsConsumerOrderRefundBean();
	}
}