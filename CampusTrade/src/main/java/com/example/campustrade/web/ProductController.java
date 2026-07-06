package com.example.campustrade.web;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.campustrade.category.CategoryService;
import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.ProductImage;
import com.example.campustrade.message.MessageForm;
import com.example.campustrade.message.MessageService;
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
		model.addAttribute("categories", categoryService.findAll());
		model.addAttribute("rootCategories", categoryService.findRootCategories());
		model.addAttribute("products", productService.searchPublicProducts(form));
		return "products/list";
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
