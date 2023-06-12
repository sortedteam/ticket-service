package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class OfferData implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	private String offerId;

	private String voucherCode;

	private Boolean isOfferApplied;

	private String offerType;

	private Double amount;

	private Double orderDiscount = 0d;

	private String offerTitle;

	public OfferData() {
		offerId = null;
		voucherCode = null;
		isOfferApplied = false;
		amount = null;
		orderDiscount = null;
		offerType = null;
		offerTitle = null;
	}

}