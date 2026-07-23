package com.example.campustrade.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

import com.example.campustrade.category.CategoryForm;
import com.example.campustrade.category.CategoryService;
import com.example.campustrade.product.ProductService;

import jakarta.validation.Valid;

@Controller
public class AdminController {

	private final ProductService productService;
	private final CategoryService categoryService;

	public AdminController(ProductService productService, CategoryService categoryService) {
		this.productService = productService;
		this.categoryService = categoryService;
	}

	@GetMapping("/admin/products")
	public String products(Model model) {
		model.addAttribute("products", productService.findAllForAdmin());
		return "admin/products";
	}

	@PostMapping("/admin/products/{id}/prohibit")
	public String prohibit(@PathVariable("id") Long id) {
		productService.prohibit(id);
		return "redirect:/admin/products";
	}

	@PostMapping("/admin/products/{id}/activate")
	public String activate(@PathVariable("id") Long id) {
		productService.activate(id);
		return "redirect:/admin/products";
	}

	@GetMapping("/admin/categories")
	public String categories(@ModelAttribute("categoryForm") CategoryForm form, Model model) {
		populateCategoryModel(model);
		return "admin/categories";
	}

	@PostMapping("/admin/categories")
	public String createCategory(@Valid @ModelAttribute("categoryForm") CategoryForm form,
			BindingResult bindingResult,
			Model model) {
		if (bindingResult.hasErrors()) {
			populateCategoryModel(model);
			return "admin/categories";
		}
		try {
			categoryService.create(form);
		} catch (ResponseStatusException ex) {
			bindingResult.reject("category", ex.getReason());
			populateCategoryModel(model);
			return "admin/categories";
		}
		return "redirect:/admin/categories";
	}

	@PostMapping("/admin/categories/{id}/edit")
	public String updateCategory(@PathVariable("id") Long id,
			@Valid @ModelAttribute("categoryForm") CategoryForm form,
			BindingResult bindingResult,
			Model model) {
		if (bindingResult.hasErrors()) {
			populateCategoryModel(model);
			return "admin/categories";
		}
		try {
			categoryService.update(id, form);
		} catch (ResponseStatusException ex) {
			bindingResult.reject("category", ex.getReason());
			populateCategoryModel(model);
			return "admin/categories";
		}
		return "redirect:/admin/categories";
	}

	@PostMapping("/admin/categories/{id}/delete")
	public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		try {
			categoryService.delete(id);
		} catch (ResponseStatusException ex) {
			redirectAttributes.addFlashAttribute("categoryError", ex.getReason());
		}
		return "redirect:/admin/categories";
	}

	private void populateCategoryModel(Model model) {
		model.addAttribute("categories", categoryService.findAll());
		model.addAttribute("rootCategories", categoryService.findRootCategories());
	}
}
