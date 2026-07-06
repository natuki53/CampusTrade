package com.example.campustrade.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.campustrade.category.CategoryService;
import com.example.campustrade.product.ProductSearchForm;
import com.example.campustrade.product.ProductService;

@Controller
public class ProductController {

	private final ProductService productService;
	private final CategoryService categoryService;

	public ProductController(ProductService productService, CategoryService categoryService) {
		this.productService = productService;
		this.categoryService = categoryService;
	}

	@GetMapping("/products")
	public String list(@ModelAttribute("searchForm") ProductSearchForm form, Model model) {
		model.addAttribute("categories", categoryService.findAll());
		model.addAttribute("rootCategories", categoryService.findRootCategories());
		model.addAttribute("products", productService.searchPublicProducts(form));
		return "products/list";
	}
}
