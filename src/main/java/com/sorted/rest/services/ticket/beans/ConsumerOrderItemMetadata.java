package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ConsumerOrderItemMetadata implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	private String productName;

	private Integer pieces;

	private String suffix;

	private Double perPiecesWeight;

	private List<ConsumerOrderItemGradeBean> grades;

}