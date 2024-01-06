package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class ConsumerOrderEta implements Serializable {

	private static final long serialVersionUID = 4988909671286229233L;

	private String maxEta;

	private Integer minEta;

	private Integer actualEta;

	private String etaText = "9 am";
}
