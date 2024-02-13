package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class ConsumerOrderItemGradeBean implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	private String name;

	private Double quantity;

	private Integer pieces;

}