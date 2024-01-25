package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ConsumerOrderMetadata implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	private Location location;

	//	private OrderContactDetail contactDetail;

	private ConsumerOrderEta eta;

	private List<ConsumerOrderProductMetadata> productMetadata;

	private List<ConsumerOrderProductMetadata> alsoContains;

	private BigDecimal calories;

	private Map<String, Long> categoryCount;

	private Double storeDistance;

	private Location storeLocation;

	private String zoneId;

	private String orderSlot;

	private List<String> failureReason;

	private Boolean gracePeriodAllowed = false;

	private BigDecimal orderPlacedAmount = null;
}