package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class FranchiseOrderMetadata implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	private List<FranchiseOrderDeliveryBean> deliveryDetails = new ArrayList<>();
}