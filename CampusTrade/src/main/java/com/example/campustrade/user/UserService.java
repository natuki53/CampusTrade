package com.example.campustrade.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.UserRole;
import com.example.campustrade.repository.AppUserRepository;

@Service
public class UserService {

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public AppUser register(RegisterForm form) {
		if (appUserRepository.existsByStudentNumber(form.getStudentNumber())) {
			throw new DuplicateStudentNumberException();
		}

		AppUser user = new AppUser();
		user.setStudentNumber(form.getStudentNumber());
		user.setPassword(passwordEncoder.encode(form.getPassword()));
		user.setNickname(form.getNickname());
		user.setRole(UserRole.STUDENT);
		return appUserRepository.save(user);
	}
}
