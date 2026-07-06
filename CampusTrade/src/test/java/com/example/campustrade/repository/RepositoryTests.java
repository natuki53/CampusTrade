package com.example.campustrade.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.Category;
import com.example.campustrade.domain.ModerationStatus;
import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.TradeStatus;
import com.example.campustrade.domain.UserRole;

@DataJpaTest
class RepositoryTests {

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Test
	void initialCategoriesAreLoaded() {
		assertThat(categoryRepository.findByParentIsNullOrderByIdAsc())
				.extracting(Category::getId)
				.containsExactly(100L, 200L, 300L, 400L, 500L);
		assertThat(categoryRepository.findByParentIdOrderByIdAsc(100L))
				.extracting(Category::getId)
				.containsExactly(110L, 120L, 130L, 140L, 190L);
	}

	@Test
	void initialAdminIsLoaded() {
		AppUser admin = appUserRepository.findByStudentNumber("admin").orElseThrow();

		assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
		assertThat(admin.getPassword()).startsWith("$2a$");
	}

	@Test
	void publicProductSearchExcludesUnavailableProducts() {
		AppUser seller = new AppUser();
		seller.setStudentNumber("s1001");
		seller.setPassword("password");
		seller.setNickname("seller");
		seller.setRole(UserRole.STUDENT);
		appUserRepository.save(seller);

		Category textbook = categoryRepository.findById(110L).orElseThrow();

		Product visible = product("統計学入門", seller, textbook, TradeStatus.OPEN, ModerationStatus.ACTIVE);
		Product locked = product("英語参考書", seller, textbook, TradeStatus.LOCKED, ModerationStatus.ACTIVE);
		Product prohibited = product("禁止商品", seller, textbook, TradeStatus.OPEN, ModerationStatus.PROHIBITED);
		productRepository.saveAll(List.of(visible, locked, prohibited));

		List<Product> results = productRepository.searchPublicProducts(null, null);

		assertThat(results).extracting(Product::getName).containsExactly("統計学入門");
	}

	private Product product(String name, AppUser seller, Category category, TradeStatus tradeStatus,
			ModerationStatus moderationStatus) {
		Product product = new Product();
		product.setSeller(seller);
		product.setCategory(category);
		product.setName(name);
		product.setDescription(name + "の説明");
		product.setPrice(1000);
		product.setConditionLabel("良好");
		product.setTradeStatus(tradeStatus);
		product.setModerationStatus(moderationStatus);
		return product;
	}
}
