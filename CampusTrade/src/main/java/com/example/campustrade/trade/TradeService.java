package com.example.campustrade.trade;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.Message;
import com.example.campustrade.domain.MessageType;
import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.TradeStatus;
import com.example.campustrade.domain.UserRole;
import com.example.campustrade.repository.AppUserRepository;
import com.example.campustrade.repository.MessageRepository;
import com.example.campustrade.repository.ProductRepository;

@Service
public class TradeService {

	private final ProductRepository productRepository;
	private final AppUserRepository appUserRepository;
	private final MessageRepository messageRepository;

	public TradeService(ProductRepository productRepository, AppUserRepository appUserRepository,
			MessageRepository messageRepository) {
		this.productRepository = productRepository;
		this.appUserRepository = appUserRepository;
		this.messageRepository = messageRepository;
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
		if (currentUser.getRole() == UserRole.ADMIN) {
			addTransactionNotice(product, currentUser, "管理者が取引をキャンセルしました。商品は出品中に戻ります。");
			reopenProduct(product);
			return product;
		}

		boolean newlyRequested = requestCancel(product, currentUser);
		clearCloseRequest(product, currentUser);
		if (product.isBuyerCancelRequested() && product.isSellerCancelRequested()) {
			addTransactionNotice(product, currentUser, "購入者と出品者のキャンセル確認がそろいました。取引をキャンセルし、商品は出品中に戻ります。");
			reopenProduct(product);
			return product;
		}
		if (newlyRequested) {
			addTransactionNotice(product, currentUser, buildWaitingMessage(product, currentUser, "キャンセル"));
		}
		return product;
	}

	@Transactional
	public Product close(Long productId, AppUser currentUser) {
		Product product = findParticipantProduct(productId, currentUser);
		if (product.getTradeStatus() != TradeStatus.LOCKED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "取引中の商品だけ完了できます");
		}
		if (currentUser.getRole() == UserRole.ADMIN) {
			addTransactionNotice(product, currentUser, "管理者が取引を完了しました。");
			resetTradeRequests(product);
			product.setTradeStatus(TradeStatus.CLOSED);
			return product;
		}

		boolean newlyRequested = requestClose(product, currentUser);
		clearCancelRequest(product, currentUser);
		if (product.isBuyerCloseRequested() && product.isSellerCloseRequested()) {
			addTransactionNotice(product, currentUser, "購入者と出品者の完了確認がそろいました。取引を完了しました。");
			resetTradeRequests(product);
			product.setTradeStatus(TradeStatus.CLOSED);
			return product;
		}
		if (newlyRequested) {
			addTransactionNotice(product, currentUser, buildWaitingMessage(product, currentUser, "取引完了"));
		}
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

	@Transactional(readOnly = true)
	public Product findBuyerTransaction(Long productId, AppUser currentUser) {
		Product product = findParticipantProduct(productId, currentUser);
		assertTransactionDetailStatus(product);
		if (currentUser.getRole() != UserRole.ADMIN
				&& (product.getBuyer() == null || !product.getBuyer().getId().equals(currentUser.getId()))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		return product;
	}

	@Transactional(readOnly = true)
	public Product findSellerTransaction(Long productId, AppUser currentUser) {
		Product product = findParticipantProduct(productId, currentUser);
		assertTransactionDetailStatus(product);
		if (currentUser.getRole() != UserRole.ADMIN && !product.getSeller().getId().equals(currentUser.getId())) {
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

	private void assertTransactionDetailStatus(Product product) {
		if (product.getTradeStatus() != TradeStatus.LOCKED && product.getTradeStatus() != TradeStatus.CLOSED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "取引詳細を表示できない商品です");
		}
	}

	private boolean requestClose(Product product, AppUser currentUser) {
		if (isBuyer(product, currentUser)) {
			boolean alreadyRequested = product.isBuyerCloseRequested();
			product.setBuyerCloseRequested(true);
			return !alreadyRequested;
		}
		if (isSeller(product, currentUser)) {
			boolean alreadyRequested = product.isSellerCloseRequested();
			product.setSellerCloseRequested(true);
			return !alreadyRequested;
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN);
	}

	private boolean requestCancel(Product product, AppUser currentUser) {
		if (isBuyer(product, currentUser)) {
			boolean alreadyRequested = product.isBuyerCancelRequested();
			product.setBuyerCancelRequested(true);
			return !alreadyRequested;
		}
		if (isSeller(product, currentUser)) {
			boolean alreadyRequested = product.isSellerCancelRequested();
			product.setSellerCancelRequested(true);
			return !alreadyRequested;
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN);
	}

	private void clearCloseRequest(Product product, AppUser currentUser) {
		if (isBuyer(product, currentUser)) {
			product.setBuyerCloseRequested(false);
		}
		else if (isSeller(product, currentUser)) {
			product.setSellerCloseRequested(false);
		}
	}

	private void clearCancelRequest(Product product, AppUser currentUser) {
		if (isBuyer(product, currentUser)) {
			product.setBuyerCancelRequested(false);
		}
		else if (isSeller(product, currentUser)) {
			product.setSellerCancelRequested(false);
		}
	}

	private void reopenProduct(Product product) {
		resetTradeRequests(product);
		product.setBuyer(null);
		product.setTradeStatus(TradeStatus.OPEN);
	}

	private void resetTradeRequests(Product product) {
		product.setBuyerCloseRequested(false);
		product.setSellerCloseRequested(false);
		product.setBuyerCancelRequested(false);
		product.setSellerCancelRequested(false);
	}

	private boolean isBuyer(Product product, AppUser currentUser) {
		return product.getBuyer() != null && product.getBuyer().getId().equals(currentUser.getId());
	}

	private boolean isSeller(Product product, AppUser currentUser) {
		return product.getSeller() != null && product.getSeller().getId().equals(currentUser.getId());
	}

	private String buildWaitingMessage(Product product, AppUser currentUser, String actionLabel) {
		String requesterLabel = isBuyer(product, currentUser) ? "購入者" : "出品者";
		String waitingLabel = isBuyer(product, currentUser) ? "出品者" : "購入者";
		return requesterLabel + "が" + actionLabel + "を申請しました。"
				+ waitingLabel + "が同じ操作を行うまでお待ちください。";
	}

	private void addTransactionNotice(Product product, AppUser sender, String content) {
		Message message = new Message();
		message.setProduct(product);
		message.setSender(appUserRepository.getReferenceById(sender.getId()));
		message.setReceiver(resolveTransactionReceiver(product, sender));
		message.setMessageType(MessageType.TRANSACTION);
		message.setContent(content);
		message.setReadFlag(false);
		messageRepository.save(message);
	}

	private AppUser resolveTransactionReceiver(Product product, AppUser sender) {
		if (isSeller(product, sender)) {
			return product.getBuyer();
		}
		if (isBuyer(product, sender)) {
			return product.getSeller();
		}
		return product.getBuyer();
	}
}
