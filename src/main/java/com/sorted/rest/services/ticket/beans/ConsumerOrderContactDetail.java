package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class ConsumerOrderContactDetail implements Serializable {

	private static final long serialVersionUID = -772465085619543410L;

	private String name;

	private String phone;
}
