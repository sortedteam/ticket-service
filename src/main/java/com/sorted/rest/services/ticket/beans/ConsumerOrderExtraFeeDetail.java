package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class ConsumerOrderExtraFeeDetail implements Serializable {

	private static final long serialVersionUID = -8555500226457969536L;

	private Double deliveryCharge = 0.0;

	private Double packingCharge = 0.0;

}
