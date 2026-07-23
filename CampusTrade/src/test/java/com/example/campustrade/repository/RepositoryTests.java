package com.example.campustrade.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
		assertThat(new BCryptPasswordEncoder().matches("password", admin.getPassword())).isTrue();
	}

	@Test
	void initialSampleUsersUseDocumentedPassword() {
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

		assertThat(appUserRepository.findByStudentNumber("s1001")).hasValueSatisfying(user ->
				assertThat(passwordEncoder.matches("password", user.getPassword())).isTrue());
	}

	@Test
	void publicProductSearchExcludesUnavailableProducts() {
		AppUser seller = new AppUser();
		seller.setStudentNumber("repo-test-seller");
		seller.setPassword("password");
		seller.setNickname("seller");
		seller.setRole(UserRole.STUDENT);
		appUserRepository.save(seller);

		Category textbook = categoryRepository.findById(110L).orElseThrow();

		Product visible = product("リポジトリ検索専用商品", seller, textbook, TradeStatus.OPEN, ModerationStatus.ACTIVE);
		Product locked = product("リポジトリ検索専用商品 取引中", seller, textbook, TradeStatus.LOCKED, ModerationStatus.ACTIVE);
		Product prohibited = product("リポジトリ検索専用商品 禁止", seller, textbook, TradeStatus.OPEN, ModerationStatus.PROHIBITED);
		Product deleted = product("リポジトリ検索専用商品 非公開", seller, textbook, TradeStatus.OPEN, ModerationStatus.ACTIVE);
		deleted.setDeletedAt(LocalDateTime.now());
		productRepository.saveAll(List.of(visible, locked, prohibited, deleted));

		List<Product> results = productRepository.searchPublicProducts("リポジトリ検索専用商品", null);

		assertThat(results).extracting(Product::getName).containsExactly("リポジトリ検索専用商品");
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
