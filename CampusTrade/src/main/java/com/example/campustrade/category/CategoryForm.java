package com.example.campustrade.category;

import com.example.campustrade.domain.Category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryForm {

	@NotNull(message = "カテゴリIDを入力してください")
	private Long id;

	private Long parentId;

	@NotBlank(message = "カテゴリ名を入力してください")
	@Size(max = 50, message = "カテゴリ名は50文字以内で入力してください")
	private String name;

	public static CategoryForm from(Category category) {
		CategoryForm form = new CategoryForm();
		form.setId(category.getId());
		form.setParentId(category.getParent() == null ? null : category.getParent().getId());
		form.setName(category.getName());
		return form;
	}
}
