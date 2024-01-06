package com.sorted.rest.services.ticket.beans;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class ConsumerOrderAdditionalDiscount implements Serializable {

	private static final long serialVersionUID = 6591657119039117441L;

	private Double offerDiscount;

}
