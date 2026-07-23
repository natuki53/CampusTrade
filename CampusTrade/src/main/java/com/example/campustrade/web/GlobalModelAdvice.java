package com.example.campustrade.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.campustrade.domain.AppUser;
import com.example.campustrade.security.CampusTradeUserDetails;

@ControllerAdvice
public class GlobalModelAdvice {

	@ModelAttribute("currentUser")
	public AppUser currentUser(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof CampusTradeUserDetails userDetails)) {
			return null;
		}
		return userDetails.getUser();
	}
}
