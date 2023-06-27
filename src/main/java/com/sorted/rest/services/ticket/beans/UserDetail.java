package com.sorted.rest.services.ticket.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserDetail implements Serializable {

	private static final long serialVersionUID = -7538803180039235801L;

	private String id;

	private String name;

	private String phone;

	private String email;

	public static UserDetail newInstance() {
		return new UserDetail();
	}
}
