package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class ConsumerOrderTaxDetails implements Serializable {

	private static final long serialVersionUID = -8369031645403533698L;

	private Double cgst = 0d;

	private Double sgst = 0d;

	private Double igst = 0d;

	private Double cessgst = 0d;

	private Double cgstAmount = 0d;

	private Double sgstAmount = 0d;

	private Double igstAmount = 0d;

	private Double cessAmount = 0d;
}
