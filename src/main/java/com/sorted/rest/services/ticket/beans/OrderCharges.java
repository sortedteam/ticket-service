package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderCharges implements Serializable {

	private static final long serialVersionUID = 6071357798367158591L;

	private String name;

	private Double amount;
}
