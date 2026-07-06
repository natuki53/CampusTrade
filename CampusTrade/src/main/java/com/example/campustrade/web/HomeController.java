package com.example.campustrade.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.campustrade.category.CategoryService;
import com.example.campustrade.product.ProductSearchForm;
import com.example.campustrade.product.ProductService;

@Controller
public class HomeController {

	private final ProductService productService;
	private final CategoryService categoryService;

	public HomeController(ProductService productService, CategoryService categoryService) {
		this.productService = productService;
		this.categoryService = categoryService;
	}

	@GetMapping("/")
	public String index(Model model) {
		var products = productService.findNewPublicProducts();
		model.addAttribute("searchForm", new ProductSearchForm());
		model.addAttribute("rootCategories", categoryService.findRootCategories());
		model.addAttribute("products", products);
		model.addAttribute("primaryImageIds", productService.findPrimaryImageIds(products));
		return "home/index";
	}
}
