package com.example.campustrade.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.Category;
import com.example.campustrade.domain.ModerationStatus;
import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.ProductImage;
import com.example.campustrade.domain.TradeStatus;
import com.example.campustrade.domain.UserRole;
import com.example.campustrade.repository.AppUserRepository;
import com.example.campustrade.repository.CategoryRepository;
import com.example.campustrade.repository.ProductImageRepository;
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

	@Autowired
	private ProductImageRepository productImageRepository;

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

	@Test
	void updateProductAddsNewImagesWithoutReplacingExistingImages() {
		AppUser seller = user("seller-4004");
		Product product = product("画像追加の商品", seller);
		ProductImage existingImage = image(product, "existing.jpg", 0, true);
		ProductForm form = ProductForm.from(product);
		form.setImages(List.of(
				new MockMultipartFile("images", "added.jpg", "image/jpeg", new byte[] {4, 5, 6})));

		productService.updateProduct(product.getId(), form, seller);

		List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
		assertThat(images).extracting(ProductImage::getId).contains(existingImage.getId());
		assertThat(images).extracting(ProductImage::getOriginalFilename)
				.containsExactly("existing.jpg", "added.jpg");
		assertThat(images).extracting(ProductImage::getPrimaryFlag)
				.containsExactly(true, false);
	}

	@Test
	void updateProductRemovesSelectedExistingImages() {
		AppUser seller = user("seller-4005");
		Product product = product("画像削除の商品", seller);
		ProductImage removedImage = image(product, "removed.jpg", 0, true);
		ProductImage remainingImage = image(product, "remaining.jpg", 1, false);
		ProductForm form = ProductForm.from(product);
		form.setRemoveImageIds(List.of(removedImage.getId()));

		productService.updateProduct(product.getId(), form, seller);

		List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
		assertThat(images).extracting(ProductImage::getId)
				.containsExactly(remainingImage.getId());
		assertThat(images.get(0).getDisplayOrder()).isZero();
		assertThat(images.get(0).getPrimaryFlag()).isTrue();
	}

	@Test
	void deleteProductImageRemovesImageAndRenumbersRemainingImages() {
		AppUser seller = user("seller-4006");
		Product product = product("画像単体削除の商品", seller);
		ProductImage removedImage = image(product, "removed.jpg", 0, true);
		ProductImage remainingImage = image(product, "remaining.jpg", 1, false);

		productService.deleteProductImage(product.getId(), removedImage.getId(), seller);

		List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
		assertThat(images).extracting(ProductImage::getId)
				.containsExactly(remainingImage.getId());
		assertThat(images.get(0).getDisplayOrder()).isZero();
		assertThat(images.get(0).getPrimaryFlag()).isTrue();
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

	private ProductImage image(Product product, String filename, int displayOrder, boolean primary) {
		ProductImage image = new ProductImage();
		image.setProduct(product);
		image.setOriginalFilename(filename);
		image.setContentType("image/jpeg");
		image.setImageData(new byte[] {1, 2, 3});
		image.setDisplayOrder(displayOrder);
		image.setPrimaryFlag(primary);
		return productImageRepository.saveAndFlush(image);
	}
}
