package com.example.campustrade.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.campustrade.repository.AppUserRepository;

@Service
public class CampusTradeUserDetailsService implements UserDetailsService {

	private final AppUserRepository appUserRepository;

	public CampusTradeUserDetailsService(AppUserRepository appUserRepository) {
		this.appUserRepository = appUserRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String studentNumber) throws UsernameNotFoundException {
		return appUserRepository.findByStudentNumber(studentNumber)
				.map(CampusTradeUserDetails::new)
				.orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません"));
	}
}
