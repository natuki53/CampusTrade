package com.example.campustrade.web;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.Product;
import com.example.campustrade.repository.AppUserRepository;
import com.example.campustrade.repository.ProductImageRepository;
import com.example.campustrade.repository.ProductRepository;
import com.example.campustrade.security.CampusTradeUserDetails;

@SpringBootTest
@AutoConfigureMockMvc
class PublicProductSecurityTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private ProductImageRepository productImageRepository;

	@Test
	void publicProductDetailIsVisibleWithoutLogin() throws Exception {
		Product product = productRepository.findByName("統計学入門 第3版").orElseThrow();

		mockMvc.perform(get("/products/{id}", product.getId()))
				.andExpect(status().isOk());
	}

	@Test
	void publicProductImageIsVisibleWithoutLogin() throws Exception {
		Product product = productRepository.findByName("統計学入門 第3版").orElseThrow();
		Long imageId = productImageRepository
				.findFirstByProductIdAndPrimaryFlagTrueOrderByDisplayOrderAsc(product.getId())
				.orElseThrow()
				.getId();

		mockMvc.perform(get("/products/{productId}/images/{imageId}", product.getId(), imageId))
				.andExpect(status().isOk())
				.andExpect(content().contentType("image/jpeg"));
	}

	@Test
	void productCreateFormRequiresLogin() throws Exception {
		mockMvc.perform(get("/products/new"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void productEditFormDisplaysExistingImagesForSeller() throws Exception {
		Product product = productRepository.findByName("統計学入門 第3版").orElseThrow();
		AppUser seller = appUserRepository.findByStudentNumber("s1001").orElseThrow();
		Long imageId = productImageRepository
				.findFirstByProductIdAndPrimaryFlagTrueOrderByDisplayOrderAsc(product.getId())
				.orElseThrow()
				.getId();

		mockMvc.perform(get("/products/{id}/edit", product.getId())
				.with(user(new CampusTradeUserDetails(seller))))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("existingImages"))
				.andExpect(content().string(containsString("/products/" + product.getId() + "/images/" + imageId)))
				.andExpect(content().string(containsString("/products/" + product.getId() + "/images/" + imageId + "/delete")))
				.andExpect(content().string(containsString("data-existing-image-delete")))
				.andExpect(content().string(containsString("name=\"_csrf\"")));
	}

	@Test
	@Transactional
	void productImageCanBeDeletedBySeller() throws Exception {
		Product product = productRepository.findByName("統計学入門 第3版").orElseThrow();
		AppUser seller = appUserRepository.findByStudentNumber("s1001").orElseThrow();
		Long imageId = productImageRepository
				.findFirstByProductIdAndPrimaryFlagTrueOrderByDisplayOrderAsc(product.getId())
				.orElseThrow()
				.getId();

		mockMvc.perform(post("/products/{productId}/images/{imageId}/delete", product.getId(), imageId)
				.with(user(new CampusTradeUserDetails(seller)))
				.with(csrf()))
				.andExpect(status().isNoContent());

		assertThat(productImageRepository.findByProductIdAndId(product.getId(), imageId)).isEmpty();
	}
}
