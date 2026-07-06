package com.example.campustrade.web;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.campustrade.category.CategoryService;
import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.ProductImage;
import com.example.campustrade.message.MessageForm;
import com.example.campustrade.message.MessageService;
import com.example.campustrade.product.InvalidProductImageException;
import com.example.campustrade.product.ProductForm;
import com.example.campustrade.product.ProductImageService;
import com.example.campustrade.product.ProductSearchForm;
import com.example.campustrade.product.ProductService;
import com.example.campustrade.repository.ProductImageRepository;
import com.example.campustrade.security.CampusTradeUserDetails;

import jakarta.validation.Valid;

@Controller
public class ProductController {

	private final ProductService productService;
	private final CategoryService categoryService;
	private final ProductImageRepository productImageRepository;
	private final ProductImageService productImageService;
	private final MessageService messageService;

	public ProductController(ProductService productService, CategoryService categoryService,
			ProductImageRepository productImageRepository, ProductImageService productImageService,
			MessageService messageService) {
		this.productService = productService;
		this.categoryService = categoryService;
		this.productImageRepository = productImageRepository;
		this.productImageService = productImageService;
		this.messageService = messageService;
	}

	@GetMapping("/products")
	public String list(@ModelAttribute("searchForm") ProductSearchForm form, Model model) {
		var products = productService.searchPublicProducts(form);
		model.addAttribute("categories", categoryService.findAll());
		model.addAttribute("rootCategories", categoryService.findRootCategories());
		model.addAttribute("products", products);
		model.addAttribute("primaryImageIds", productService.findPrimaryImageIds(products));
		return "products/list";
	}

	@GetMapping("/products/new")
	public String newForm(@ModelAttribute("productForm") ProductForm form, Model model) {
		model.addAttribute("categories", categoryService.findSelectableCategories());
		model.addAttribute("mode", "new");
		return "products/form";
	}

	@PostMapping("/products")
	public String create(@AuthenticationPrincipal CampusTradeUserDetails userDetails,
			@Valid @ModelAttribute("productForm") ProductForm form,
			BindingResult bindingResult,
			Model model) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("categories", categoryService.findSelectableCategories());
			model.addAttribute("mode", "new");
			return "products/form";
		}

		try {
			Product product = productService.createProduct(form, userDetails.getUser());
			return "redirect:/products/" + product.getId();
		} catch (InvalidProductImageException ex) {
			bindingResult.reject("images", ex.getMessage());
			model.addAttribute("categories", categoryService.findSelectableCategories());
			model.addAttribute("mode", "new");
			return "products/form";
		}
	}

	@GetMapping("/products/{id}/edit")
	public String editForm(@PathVariable Long id, @AuthenticationPrincipal CampusTradeUserDetails userDetails,
			Model model) {
		Product product = productService.findEditableProduct(id, userDetails.getUser());
		model.addAttribute("product", product);
		model.addAttribute("productForm", ProductForm.from(product));
		model.addAttribute("categories", categoryService.findSelectableCategories());
		model.addAttribute("mode", "edit");
		return "products/form";
	}

	@PostMapping("/products/{id}/edit")
	public String update(@PathVariable Long id, @AuthenticationPrincipal CampusTradeUserDetails userDetails,
			@Valid @ModelAttribute("productForm") ProductForm form,
			BindingResult bindingResult,
			Model model) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("product", productService.findEditableProduct(id, userDetails.getUser()));
			model.addAttribute("categories", categoryService.findSelectableCategories());
			model.addAttribute("mode", "edit");
			return "products/form";
		}

		try {
			productService.updateProduct(id, form, userDetails.getUser());
			return "redirect:/products/" + id;
		} catch (InvalidProductImageException ex) {
			bindingResult.reject("images", ex.getMessage());
			model.addAttribute("product", productService.findEditableProduct(id, userDetails.getUser()));
			model.addAttribute("categories", categoryService.findSelectableCategories());
			model.addAttribute("mode", "edit");
			return "products/form";
		}
	}

	@PostMapping("/products/{id}/delete")
	public String delete(@PathVariable Long id, @AuthenticationPrincipal CampusTradeUserDetails userDetails) {
		productService.softDelete(id, userDetails.getUser());
		return "redirect:/mypage";
	}

	@GetMapping("/products/{id}")
	public String detail(@PathVariable Long id, @AuthenticationPrincipal CampusTradeUserDetails userDetails,
			Model model) {
		AppUser viewer = userDetails == null ? null : userDetails.getUser();
		Product product = productService.findViewableProduct(id, viewer);
		model.addAttribute("product", product);
		model.addAttribute("images", productImageRepository.findByProductIdOrderByDisplayOrderAsc(id));
		model.addAttribute("comments", messageService.findComments(id));
		model.addAttribute("messageForm", new MessageForm());
		return "products/detail";
	}

	@GetMapping("/products/{productId}/images/{imageId}")
	public ResponseEntity<byte[]> image(@PathVariable Long productId, @PathVariable Long imageId,
			@AuthenticationPrincipal CampusTradeUserDetails userDetails) {
		AppUser viewer = userDetails == null ? null : userDetails.getUser();
		ProductImage image = productImageService.findViewableImage(productId, imageId, viewer);
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noCache())
				.contentType(MediaType.parseMediaType(image.getContentType()))
				.body(image.getImageData());
	}

	@PostMapping("/products/{id}/comments")
	public String addComment(@PathVariable Long id,
			@AuthenticationPrincipal CampusTradeUserDetails userDetails,
			@Valid @ModelAttribute("messageForm") MessageForm form,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("commentError", "コメントを入力してください");
			return "redirect:/products/" + id;
		}

		messageService.addComment(id, userDetails.getUser(), form);
		return "redirect:/products/" + id;
	}
}
