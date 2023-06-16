package com.sorted.rest.services.ticket.beans;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class WalletStatementBean implements Serializable {

	private Integer id;

	private Double amount;

	private Double balance;

	private String txnMode;

	private String txnType;

	private String txnDetail;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "IST")
	private Date createdAt;

	private String remarks;

	public static WalletStatementBean newInstance() {
		return new WalletStatementBean();
	}
}
