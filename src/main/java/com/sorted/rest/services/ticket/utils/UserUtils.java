package com.sorted.rest.services.ticket.utils;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.services.ticket.beans.UserDetail;
import com.sorted.rest.services.ticket.beans.UserServiceResponse;
import com.sorted.rest.services.ticket.clients.TicketClientService;
import com.sorted.rest.services.ticket.constants.TicketConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserUtils {

	@Autowired
	public TicketClientService ticketClientService;

	@Value("${auth.id}")
	private UUID internalAuthUserId;

	public UserDetail getInternalUserDetail() {
		UserDetail userDetail = UserDetail.newInstance();
		userDetail.setId(internalAuthUserId.toString());
		userDetail.setName(TicketConstants.INTERNAL_USER_NAME);
		userDetail.setPhone(null);
		userDetail.setEmail(null);
		return userDetail;
	}

	public UserDetail getUserDetail(UUID userId) {
		if (userId.equals(internalAuthUserId)) {
			return getInternalUserDetail();
		}

		UserServiceResponse entity = ticketClientService.getUserDetailsFromCustomerId(userId);
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
