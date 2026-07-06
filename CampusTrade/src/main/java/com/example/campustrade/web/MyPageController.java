package com.example.campustrade.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.TradeStatus;
import com.example.campustrade.product.ProductService;
import com.example.campustrade.security.CampusTradeUserDetails;

@Controller
public class MyPageController {

	private final ProductService productService;

	public MyPageController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping("/mypage")
	public String index(@AuthenticationPrincipal CampusTradeUserDetails userDetails, Model model) {
		List<Product> purchases = productService.findPurchases(userDetails.getUser());
		List<Product> sales = productService.findSales(userDetails.getUser());
		model.addAttribute("purchaseCount", purchases.size());
		model.addAttribute("saleCount", sales.size());
		model.addAttribute("activeTradeCount", countActiveTrades(purchases) + countActiveTrades(sales));
		return "mypage/index";
	}

	@GetMapping("/mypage/purchases")
	public String purchases(@AuthenticationPrincipal CampusTradeUserDetails userDetails, Model model) {
		model.addAttribute("products", productService.findPurchases(userDetails.getUser()));
		return "mypage/purchases";
	}

	@GetMapping("/mypage/sales")
	public String sales(@AuthenticationPrincipal CampusTradeUserDetails userDetails, Model model) {
		model.addAttribute("products", productService.findSales(userDetails.getUser()));
		return "mypage/sales";
	}

	private long countActiveTrades(List<Product> products) {
		return products.stream()
				.filter(product -> product.getTradeStatus() == TradeStatus.LOCKED)
				.count();
	}
}
