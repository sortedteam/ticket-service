package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class WhSkuResponse implements Serializable {

	private static final long serialVersionUID = -1365034136062032242L;

	private Integer whId;

	private String skuCode;

	private Double permissibleRefundQuantity;

}
