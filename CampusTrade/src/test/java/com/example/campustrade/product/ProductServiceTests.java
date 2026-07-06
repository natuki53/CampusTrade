package com.example.campustrade.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.Category;
import com.example.campustrade.domain.ModerationStatus;
import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.TradeStatus;
import com.example.campustrade.domain.UserRole;
import com.example.campustrade.repository.AppUserRepository;
import com.example.campustrade.repository.CategoryRepository;
import com.example.campustrade.repository.ProductRepository;

@SpringBootTest
@Transactional
class ProductServiceTests {

	@Autowired
	private ProductService productService;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Test
	void salesListIncludesDeletedProducts() {
		AppUser seller = user("seller-4001");
		Product deletedProduct = product("非公開の商品", seller);
		deletedProduct.setDeletedAt(LocalDateTime.now());
		productRepository.saveAndFlush(deletedProduct);

		assertThat(productService.findSales(seller))
				.extracting(Product::getName)
				.contains("非公開の商品");
	}

	@Test
	void publishClearsDeletedAt() {
		AppUser seller = user("seller-4002");
		Product product = product("再公開する商品", seller);
		product.setDeletedAt(LocalDateTime.now());
		productRepository.saveAndFlush(product);

		productService.publish(product.getId(), seller);

		assertThat(product.getDeletedAt()).isNull();
		assertThat(product.isVisibleToPublic()).isTrue();
	}

	@Test
	void publishRejectsProhibitedProduct() {
		AppUser seller = user("seller-4003");
		Product product = product("禁止中の商品", seller);
		product.setDeletedAt(LocalDateTime.now());
		product.setModerationStatus(ModerationStatus.PROHIBITED);
		productRepository.saveAndFlush(product);

		assertThatThrownBy(() -> productService.publish(product.getId(), seller))
				.isInstanceOf(ResponseStatusException.class);
	}

	private AppUser user(String studentNumber) {
		AppUser user = new AppUser();
		user.setStudentNumber(studentNumber);
		user.setPassword("password");
		user.setNickname(studentNumber);
		user.setRole(UserRole.STUDENT);
		return appUserRepository.save(user);
	}

	private Product product(String name, AppUser seller) {
		Category category = categoryRepository.findById(110L).orElseThrow();
		Product product = new Product();
		product.setSeller(seller);
		product.setCategory(category);
		product.setName(name);
		product.setDescription(name + "の説明");
		product.setPrice(1000);
		product.setConditionLabel("良好");
		product.setTradeStatus(TradeStatus.OPEN);
		product.setModerationStatus(ModerationStatus.ACTIVE);
		return productRepository.saveAndFlush(product);
	}
}
