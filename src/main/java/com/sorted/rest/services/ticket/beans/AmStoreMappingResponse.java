package com.sorted.rest.services.ticket.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class AmStoreMappingResponse {

	private String storeId;

	private AmUserDetails amUser;

	@Data
	public static class AmUserDetails implements Serializable {

		private static final long serialVersionUID = -7538803180039235801L;

		private String id;

		private String name;

		@JsonProperty("phone_number")
		private String phone;

		private String email;

		private String errorMsg;

		public static AmUserDetails newInstance() {
			return new AmUserDetails();
		}
	}

}


