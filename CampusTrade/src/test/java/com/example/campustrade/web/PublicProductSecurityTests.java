package com.example.campustrade.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.example.campustrade.domain.Product;
import com.example.campustrade.repository.ProductImageRepository;
import com.example.campustrade.repository.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
class PublicProductSecurityTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

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
}
