package com.example.campustrade.product;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.campustrade.domain.Product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductForm {

	@NotBlank(message = "商品名を入力してください")
	@Size(max = 100, message = "商品名は100文字以内で入力してください")
	private String name;

	@NotBlank(message = "説明を入力してください")
	private String description;

	@NotNull(message = "価格を入力してください")
	@Min(value = 0, message = "価格は0円以上で入力してください")
	private Integer price;

	@NotNull(message = "カテゴリを選択してください")
	private Long categoryId;

	@NotBlank(message = "商品状態を入力してください")
	@Size(max = 50, message = "商品状態は50文字以内で入力してください")
	private String conditionLabel;

	private List<MultipartFile> images = new ArrayList<>();

	private List<Long> removeImageIds = new ArrayList<>();

	public static ProductForm from(Product product) {
		ProductForm form = new ProductForm();
		form.setName(product.getName());
		form.setDescription(product.getDescription());
		form.setPrice(product.getPrice());
		form.setCategoryId(product.getCategory().getId());
		form.setConditionLabel(product.getConditionLabel());
		return form;
	}
}
