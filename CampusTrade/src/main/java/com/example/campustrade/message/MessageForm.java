package com.example.campustrade.message;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageForm {

	@NotBlank(message = "メッセージを入力してください")
	private String content;
}
