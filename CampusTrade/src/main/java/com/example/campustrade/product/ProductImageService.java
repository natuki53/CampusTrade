package com.example.campustrade.product;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.ProductImage;
import com.example.campustrade.repository.ProductImageRepository;

@Service
public class ProductImageService {

	private final ProductService productService;
	private final ProductImageRepository productImageRepository;

	public ProductImageService(ProductService productService, ProductImageRepository productImageRepository) {
		this.productService = productService;
		this.productImageRepository = productImageRepository;
	}

	@Transactional(readOnly = true)
	public ProductImage findViewableImage(Long productId, Long imageId, AppUser viewer) {
		Product product = productService.findViewableProduct(productId, viewer);
		return productImageRepository.findByProductIdAndId(product.getId(), imageId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}
}
