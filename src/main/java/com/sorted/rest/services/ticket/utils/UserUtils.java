package com.sorted.rest.services.ticket.utils;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.services.ticket.beans.UserDetail;
import com.sorted.rest.services.ticket.beans.UserServiceResponse;
import com.sorted.rest.services.ticket.clients.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserUtils {

	@Autowired
	public ClientService clientService;

	public UserDetail getUserDetail(UUID userId) {
		UserServiceResponse entity = clientService.getUserDetailsFromCustomerId(userId);
		if (entity == null)
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "User details not found", ""));
		UserDetail userDetail = UserDetail.newInstance();
		userDetail.setName(entity.getName());
		userDetail.setPhone(entity.getPhoneNumber());
		userDetail.setEmail(entity.getEmail());
		userDetail.setId(entity.getId());
		return userDetail;
	}
}
