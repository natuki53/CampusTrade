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
import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.TradeStatus;
import com.example.campustrade.domain.UserRole;
import com.example.campustrade.repository.AppUserRepository;
import com.example.campustrade.repository.CategoryRepository;
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
	void cancelReopensLockedProduct() {
		AppUser seller = user("seller-3003");
		AppUser buyer = user("buyer-3003");
		Product product = product(seller);
		tradeService.purchase(product.getId(), buyer);

		Product canceled = tradeService.cancel(product.getId(), buyer);

		assertThat(canceled.getTradeStatus()).isEqualTo(TradeStatus.OPEN);
		assertThat(canceled.getBuyer()).isNull();
	}

	@Test
	void closeCompletesLockedProduct() {
		AppUser seller = user("seller-3004");
		AppUser buyer = user("buyer-3004");
		Product product = product(seller);
		tradeService.purchase(product.getId(), buyer);

		Product closed = tradeService.close(product.getId(), seller);

		assertThat(closed.getTradeStatus()).isEqualTo(TradeStatus.CLOSED);
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
