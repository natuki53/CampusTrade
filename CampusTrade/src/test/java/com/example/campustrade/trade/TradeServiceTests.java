package com.example.campustrade.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.Category;
import com.example.campustrade.domain.Message;
import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.TradeStatus;
import com.example.campustrade.domain.UserRole;
import com.example.campustrade.repository.AppUserRepository;
import com.example.campustrade.repository.CategoryRepository;
import com.example.campustrade.repository.MessageRepository;
import com.example.campustrade.repository.ProductRepository;

@SpringBootTest
@Transactional
class TradeServiceTests {

	@Autowired
	private TradeService tradeService;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private MessageRepository messageRepository;

	@Test
	void purchaseLocksOpenProduct() {
		AppUser seller = user("seller-3001");
		AppUser buyer = user("buyer-3001");
		Product product = product(seller);

		Product purchased = tradeService.purchase(product.getId(), buyer);

		assertThat(purchased.getTradeStatus()).isEqualTo(TradeStatus.LOCKED);
		assertThat(purchased.getBuyer().getId()).isEqualTo(buyer.getId());
	}

	@Test
	void purchaseRejectsOwnProduct() {
		AppUser seller = user("seller-3002");
		Product product = product(seller);

		assertThatThrownBy(() -> tradeService.purchase(product.getId(), seller))
				.isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void cancelReopensProductAfterBothParticipantsRequestCancel() {
		AppUser seller = user("seller-3003");
		AppUser buyer = user("buyer-3003");
		Product product = product(seller);
		tradeService.purchase(product.getId(), buyer);

		Product firstRequested = tradeService.cancel(product.getId(), buyer);

		assertThat(firstRequested.getTradeStatus()).isEqualTo(TradeStatus.LOCKED);
		assertThat(firstRequested.isBuyerCancelRequested()).isTrue();
		assertThat(firstRequested.getBuyer()).isNotNull();
		assertThat(messageContents(product))
				.contains("購入者がキャンセルを申請しました。出品者が同じ操作を行うまでお待ちください。");

		Product canceled = tradeService.cancel(product.getId(), seller);

		assertThat(canceled.getTradeStatus()).isEqualTo(TradeStatus.OPEN);
		assertThat(canceled.getBuyer()).isNull();
		assertThat(canceled.isBuyerCancelRequested()).isFalse();
		assertThat(canceled.isSellerCancelRequested()).isFalse();
	}

	@Test
	void closeCompletesProductAfterBothParticipantsRequestClose() {
		AppUser seller = user("seller-3004");
		AppUser buyer = user("buyer-3004");
		Product product = product(seller);
		tradeService.purchase(product.getId(), buyer);

		Product firstRequested = tradeService.close(product.getId(), seller);

		assertThat(firstRequested.getTradeStatus()).isEqualTo(TradeStatus.LOCKED);
		assertThat(firstRequested.isSellerCloseRequested()).isTrue();
		assertThat(messageContents(product))
				.contains("出品者が取引完了を申請しました。購入者が同じ操作を行うまでお待ちください。");

		Product closed = tradeService.close(product.getId(), buyer);

		assertThat(closed.getTradeStatus()).isEqualTo(TradeStatus.CLOSED);
		assertThat(closed.isBuyerCloseRequested()).isFalse();
		assertThat(closed.isSellerCloseRequested()).isFalse();
	}

	private Iterable<String> messageContents(Product product) {
		return messageRepository.findTransactionMessagesForAdmin(product.getId()).stream()
				.map(Message::getContent)
				.toList();
	}

	private AppUser user(String studentNumber) {
		AppUser user = new AppUser();
		user.setStudentNumber(studentNumber);
		user.setPassword("password");
		user.setNickname(studentNumber);
		user.setRole(UserRole.STUDENT);
		return appUserRepository.save(user);
	}

	private Product product(AppUser seller) {
		Category category = categoryRepository.findById(110L).orElseThrow();
		Product product = new Product();
		product.setSeller(seller);
		product.setCategory(category);
		product.setName("取引テスト商品");
		product.setDescription("説明");
		product.setPrice(1000);
		product.setConditionLabel("良好");
		return productRepository.saveAndFlush(product);
	}
}
