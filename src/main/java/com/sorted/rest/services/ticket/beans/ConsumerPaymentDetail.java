package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class ConsumerPaymentDetail implements Serializable {

	private static final long serialVersionUID = 2989920741806143595L;

	private String paymentStatus;

	private String paymentGateway;

	private List<UUID> transactions;

	private Double pendingAmount;
}
