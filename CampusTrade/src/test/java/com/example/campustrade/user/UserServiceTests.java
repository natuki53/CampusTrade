package com.example.campustrade.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.UserRole;
import com.example.campustrade.repository.AppUserRepository;

@SpringBootTest
class UserServiceTests {

	@Autowired
	private UserService userService;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void registerCreatesStudentWithHashedPassword() {
		RegisterForm form = new RegisterForm();
		form.setStudentNumber("s2001");
		form.setPassword("secret");
		form.setNickname("なつ");

		AppUser user = userService.register(form);

		assertThat(user.getRole()).isEqualTo(UserRole.STUDENT);
		assertThat(user.getPassword()).isNotEqualTo("secret");
		assertThat(passwordEncoder.matches("secret", user.getPassword())).isTrue();
	}

	@Test
	void registerRejectsDuplicateStudentNumber() {
		RegisterForm form = new RegisterForm();
		form.setStudentNumber("s2002");
		form.setPassword("secret");
		form.setNickname("なつ");
		userService.register(form);

		RegisterForm duplicate = new RegisterForm();
		duplicate.setStudentNumber("s2002");
		duplicate.setPassword("secret");
		duplicate.setNickname("別ユーザー");

		assertThatThrownBy(() -> userService.register(duplicate))
				.isInstanceOf(DuplicateStudentNumberException.class);
		assertThat(appUserRepository.findByStudentNumber("s2002")).isPresent();
	}
}
