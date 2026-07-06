package com.example.campustrade.product;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.campustrade.category.CategoryService;
import com.example.campustrade.domain.Product;
import com.example.campustrade.repository.ProductRepository;

@Service
public class ProductService {

	private final ProductRepository productRepository;
	private final CategoryService categoryService;

	public ProductService(ProductRepository productRepository, CategoryService categoryService) {
		this.productRepository = productRepository;
		this.categoryService = categoryService;
	}

	@Transactional(readOnly = true)
	public List<Product> searchPublicProducts(ProductSearchForm form) {
		List<Long> categoryIds = categoryService.resolveSearchCategoryIds(form.getCategoryId());
		return productRepository.searchPublicProducts(form.normalizedKeyword(), categoryIds);
	}

	@Transactional(readOnly = true)
	public List<Product> findNewPublicProducts() {
		return productRepository.searchPublicProducts(null, null).stream()
				.limit(8)
				.toList();
	}
}
