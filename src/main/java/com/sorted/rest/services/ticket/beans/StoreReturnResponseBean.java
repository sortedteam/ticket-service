package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class StoreReturnResponseBean implements Serializable {

	private static final long serialVersionUID = -6385345671727969023L;

	private int id;

	private int storeId;

	private int whId;

	private Date returnDate;

	private String status;

	private String createdBy;

	private Date createdOn;

	private String modifiedBy;

	private Date modifiedOn;

	private String orderId;

	private List<StoreReturnItemData> storeReturnItemDataList;

	public static StoreReturnResponseBean newInstance() {
		return new StoreReturnResponseBean();
	}
}
