package com.example.campustrade.web;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.campustrade.user.DuplicateStudentNumberException;
import com.example.campustrade.user.RegisterForm;
import com.example.campustrade.user.UserService;

import jakarta.validation.Valid;

@Controller
public class AuthController {

	private final UserService userService;

	public AuthController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/login")
	public String login() {
		return "auth/login";
	}

	@GetMapping("/register")
	public String registerForm(@ModelAttribute("registerForm") RegisterForm form) {
		return "auth/register";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute("registerForm") RegisterForm form, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return "auth/register";
		}

		try {
			userService.register(form);
		} catch (DuplicateStudentNumberException ex) {
			bindingResult.rejectValue("studentNumber", "duplicate", "この学生番号はすでに登録されています");
			return "auth/register";
		}

		redirectAttributes.addAttribute("registered", true);
		return "redirect:/login";
	}
}
