package com.sorted.rest.services.ticket.beans;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class TargetCashbackCronRequest implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	private String date;

	private List<String> storeIds;

}