package com.example.campustrade.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.TradeStatus;
import com.example.campustrade.message.MessageForm;
import com.example.campustrade.message.MessageService;
import com.example.campustrade.product.ProductService;
import com.example.campustrade.security.CampusTradeUserDetails;
import com.example.campustrade.trade.TradeService;

import jakarta.validation.Valid;

@Controller
public class MyPageController {

	private final ProductService productService;
	private final TradeService tradeService;
	private final MessageService messageService;

	public MyPageController(ProductService productService, TradeService tradeService, MessageService messageService) {
		this.productService = productService;
		this.tradeService = tradeService;
		this.messageService = messageService;
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

	@PostMapping("/mypage/sales/{productId}/publish")
	public String publishSale(@PathVariable Long productId,
			@AuthenticationPrincipal CampusTradeUserDetails userDetails,
			RedirectAttributes redirectAttributes) {
		productService.publish(productId, userDetails.getUser());
		redirectAttributes.addFlashAttribute("successMessage", "商品を公開しました");
		return "redirect:/mypage/sales";
	}

	@GetMapping("/mypage/purchases/{productId}")
	public String purchaseDetail(@PathVariable Long productId,
			@AuthenticationPrincipal CampusTradeUserDetails userDetails,
			Model model) {
		Product product = tradeService.findBuyerTransaction(productId, userDetails.getUser());
		populateTransactionModel(model, product, userDetails, "buyer");
		return "mypage/transaction";
	}

	@GetMapping("/mypage/sales/{productId}")
	public String saleDetail(@PathVariable Long productId,
			@AuthenticationPrincipal CampusTradeUserDetails userDetails,
			Model model) {
		Product product = tradeService.findSellerTransaction(productId, userDetails.getUser());
		populateTransactionModel(model, product, userDetails, "seller");
		return "mypage/transaction";
	}

	@PostMapping("/mypage/purchases/{productId}/messages")
	public String sendPurchaseMessage(@PathVariable Long productId,
			@AuthenticationPrincipal CampusTradeUserDetails userDetails,
			@Valid @ModelAttribute("messageForm") MessageForm form,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		Product product = tradeService.findBuyerTransaction(productId, userDetails.getUser());
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("messageError", "メッセージを入力してください");
			return "redirect:/mypage/purchases/" + productId;
		}
		messageService.addTransactionMessage(product, userDetails.getUser(), form);
		return "redirect:/mypage/purchases/" + productId;
	}

	@PostMapping("/mypage/sales/{productId}/messages")
	public String sendSaleMessage(@PathVariable Long productId,
			@AuthenticationPrincipal CampusTradeUserDetails userDetails,
			@Valid @ModelAttribute("messageForm") MessageForm form,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		Product product = tradeService.findSellerTransaction(productId, userDetails.getUser());
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("messageError", "メッセージを入力してください");
			return "redirect:/mypage/sales/" + productId;
		}
		messageService.addTransactionMessage(product, userDetails.getUser(), form);
		return "redirect:/mypage/sales/" + productId;
	}

	private long countActiveTrades(List<Product> products) {
		return products.stream()
				.filter(product -> product.getTradeStatus() == TradeStatus.LOCKED)
				.count();
	}

	private void populateTransactionModel(Model model, Product product, CampusTradeUserDetails userDetails, String mode) {
		model.addAttribute("product", product);
		model.addAttribute("mode", mode);
		model.addAttribute("messages", messageService.findTransactionMessages(product, userDetails.getUser()));
		model.addAttribute("messageForm", new MessageForm());
		model.addAttribute("closeRequestedByCurrentUser", isCloseRequestedByCurrentUser(product, mode));
		model.addAttribute("cancelRequestedByCurrentUser", isCancelRequestedByCurrentUser(product, mode));
		model.addAttribute("tradeActionNotices", buildTradeActionNotices(product, mode));
		model.addAttribute("backUrl", "buyer".equals(mode) ? "/mypage/purchases" : "/mypage/sales");
		model.addAttribute("messageUrl", "buyer".equals(mode)
				? "/mypage/purchases/" + product.getId() + "/messages"
				: "/mypage/sales/" + product.getId() + "/messages");
	}

	private boolean isCloseRequestedByCurrentUser(Product product, String mode) {
		if ("buyer".equals(mode)) {
			return product.isBuyerCloseRequested();
		}
		return product.isSellerCloseRequested();
	}

	private boolean isCancelRequestedByCurrentUser(Product product, String mode) {
		if ("buyer".equals(mode)) {
			return product.isBuyerCancelRequested();
		}
		return product.isSellerCancelRequested();
	}

	private List<String> buildTradeActionNotices(Product product, String mode) {
		List<String> notices = new ArrayList<>();
		if (product.getTradeStatus() != TradeStatus.LOCKED) {
			return notices;
		}
		boolean buyerView = "buyer".equals(mode);
		addRequestNotice(notices, product.isBuyerCloseRequested(), buyerView, "購入者", "出品者", "取引完了");
		addRequestNotice(notices, product.isSellerCloseRequested(), !buyerView, "出品者", "購入者", "取引完了");
		addRequestNotice(notices, product.isBuyerCancelRequested(), buyerView, "購入者", "出品者", "キャンセル");
		addRequestNotice(notices, product.isSellerCancelRequested(), !buyerView, "出品者", "購入者", "キャンセル");
		return notices;
	}

	private void addRequestNotice(List<String> notices, boolean requested, boolean currentUserRequested,
			String requesterLabel, String waitingLabel, String actionLabel) {
		if (!requested) {
			return;
		}
		if (currentUserRequested) {
			notices.add(actionLabel + "を申請済みです。" + waitingLabel + "が同じ操作を行うまでお待ちください。");
			return;
		}
		notices.add(requesterLabel + "が" + actionLabel + "を申請しています。問題なければ同じ操作を押してください。");
	}
}
