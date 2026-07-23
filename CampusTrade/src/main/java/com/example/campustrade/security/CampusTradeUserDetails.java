package com.example.campustrade.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.campustrade.domain.AppUser;

public class CampusTradeUserDetails implements UserDetails {

	private final AppUser user;

	public CampusTradeUserDetails(AppUser user) {
		this.user = user;
	}

	public AppUser getUser() {
		return user;
	}

	public Long getId() {
		return user.getId();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getUsername() {
		return user.getStudentNumber();
	}
}
