package com.example.campustrade.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterForm {

	@NotBlank(message = "学生番号を入力してください")
	@Size(max = 32, message = "学生番号は32文字以内で入力してください")
	private String studentNumber;

	@NotBlank(message = "パスワードを入力してください")
	private String password;

	@NotBlank(message = "ニックネームを入力してください")
	@Size(max = 50, message = "ニックネームは50文字以内で入力してください")
	private String nickname;
}
