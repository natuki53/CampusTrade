package com.example.campustrade.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.UserRole;
import com.example.campustrade.security.CampusTradeUserDetails;
import com.example.campustrade.trade.TradeService;

@Controller
public class TradeController {

	private final TradeService tradeService;

	public TradeController(TradeService tradeService) {
		this.tradeService = tradeService;
	}

	@PostMapping("/products/{id}/purchase")
	public String purchase(@PathVariable("id") Long id, @AuthenticationPrincipal CampusTradeUserDetails userDetails) {
		Product product = tradeService.purchase(id, userDetails.getUser());
		return "redirect:/mypage/purchases/" + product.getId();
	}

	@PostMapping("/products/{id}/cancel")
	public String cancel(@PathVariable("id") Long id, @AuthenticationPrincipal CampusTradeUserDetails userDetails) {
		Product product = tradeService.cancel(id, userDetails.getUser());
		return redirectAfterCancel(product, userDetails.getUser());
	}

	@PostMapping("/products/{id}/close")
	public String close(@PathVariable("id") Long id, @AuthenticationPrincipal CampusTradeUserDetails userDetails) {
		Product product = tradeService.close(id, userDetails.getUser());
		return redirectAfterClose(product, userDetails.getUser());
	}

	private String redirectAfterCancel(Product product, AppUser currentUser) {
		if (currentUser.getRole() == UserRole.ADMIN) {
			return "redirect:/admin/products";
		}
		if (product.getSeller().getId().equals(currentUser.getId())) {
			return "redirect:/mypage/sales";
		}
		return "redirect:/mypage/purchases";
	}

	private String redirectAfterClose(Product product, AppUser currentUser) {
		if (currentUser.getRole() == UserRole.ADMIN) {
			return "redirect:/admin/products";
		}
		if (product.getSeller().getId().equals(currentUser.getId())) {
			return "redirect:/mypage/sales/" + product.getId();
		}
		return "redirect:/mypage/purchases/" + product.getId();
	}
}
