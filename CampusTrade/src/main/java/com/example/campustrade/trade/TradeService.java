package com.example.campustrade.trade;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.TradeStatus;
import com.example.campustrade.domain.UserRole;
import com.example.campustrade.repository.AppUserRepository;
import com.example.campustrade.repository.ProductRepository;

@Service
public class TradeService {

	private final ProductRepository productRepository;
	private final AppUserRepository appUserRepository;

	public TradeService(ProductRepository productRepository, AppUserRepository appUserRepository) {
		this.productRepository = productRepository;
		this.appUserRepository = appUserRepository;
	}

	@Transactional
	public Product purchase(Long productId, AppUser currentUser) {
		AppUser buyer = appUserRepository.getReferenceById(currentUser.getId());
		int updated = productRepository.lockForPurchase(productId, buyer, currentUser.getId(), LocalDateTime.now());
		if (updated == 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "購入申し込みできない商品です");
		}
		return productRepository.findDetailById(productId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	@Transactional
	public Product cancel(Long productId, AppUser currentUser) {
		Product product = findParticipantProduct(productId, currentUser);
		if (product.getTradeStatus() != TradeStatus.LOCKED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "取引中の商品だけキャンセルできます");
		}
		product.setBuyer(null);
		product.setTradeStatus(TradeStatus.OPEN);
		return product;
	}

	@Transactional
	public Product close(Long productId, AppUser currentUser) {
		Product product = findParticipantProduct(productId, currentUser);
		if (product.getTradeStatus() != TradeStatus.LOCKED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "取引中の商品だけ完了できます");
		}
		product.setTradeStatus(TradeStatus.CLOSED);
		return product;
	}

	@Transactional(readOnly = true)
	public Product findParticipantProduct(Long productId, AppUser currentUser) {
		Product product = productRepository.findDetailById(productId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (!isParticipant(product, currentUser)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		return product;
	}

	public boolean isParticipant(Product product, AppUser currentUser) {
		if (currentUser == null) {
			return false;
		}
		if (currentUser.getRole() == UserRole.ADMIN) {
			return true;
		}
		if (product.getSeller() != null && product.getSeller().getId().equals(currentUser.getId())) {
			return true;
		}
		return product.getBuyer() != null && product.getBuyer().getId().equals(currentUser.getId());
	}
}
