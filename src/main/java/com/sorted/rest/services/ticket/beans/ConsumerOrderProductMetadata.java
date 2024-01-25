package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class ConsumerOrderProductMetadata implements Serializable {

	private static final long serialVersionUID = -5081176278171548186L;

	private Double value;

	private String name;

	private String color;

}
