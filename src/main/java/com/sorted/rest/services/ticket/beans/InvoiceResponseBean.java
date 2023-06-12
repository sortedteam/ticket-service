package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class InvoiceResponseBean implements Serializable {

	private Integer id;

	private String invoiceName;

	private String invoiceUrl;

	private String invoiceType;

	private Double amount;

	private PaymentNoteDetails paymentNotes;

}
