package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class ConsumerOrderMetadata implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	private ConsumerOrderContactDetail contactDetail;

	private String orderSlot;
}