package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class PaymentNoteData implements Serializable {

	private String name;

	private String type;

	private String url;

	private Date date;

	private Double amount;

	public static PaymentNoteData newInstance() {
		return new PaymentNoteData();
	}

	public static PaymentNoteData createPaymentNoteData(String name, String invoiceType, String invoiceUrl, Date date, Double invoiceAmount) {
		PaymentNoteData paymentNoteData = newInstance();
		paymentNoteData.setName(name);
		paymentNoteData.setUrl(invoiceUrl);
		paymentNoteData.setDate(date);
		paymentNoteData.setType(invoiceType);
		paymentNoteData.setAmount(invoiceAmount);
		return paymentNoteData;
	}
}
