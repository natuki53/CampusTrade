package com.example.campustrade.product;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchForm {

	private String keyword;

	private Long categoryId;

	public String normalizedKeyword() {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}
		return keyword.trim();
	}
}
