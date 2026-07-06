package com.example.campustrade.product;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.campustrade.category.CategoryService;
import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.UserRole;
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

	@Transactional(readOnly = true)
	public Product findViewableProduct(Long id, AppUser viewer) {
		Product product = productRepository.findDetailById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (product.isVisibleToPublic() || canViewRestrictedProduct(product, viewer)) {
			return product;
		}
		throw new ResponseStatusException(HttpStatus.NOT_FOUND);
	}

	public boolean canViewRestrictedProduct(Product product, AppUser viewer) {
		if (viewer == null) {
			return false;
		}
		if (viewer.getRole() == UserRole.ADMIN) {
			return true;
		}
		if (product.getSeller() != null && product.getSeller().getId().equals(viewer.getId())) {
			return true;
		}
		return product.getBuyer() != null && product.getBuyer().getId().equals(viewer.getId());
	}
}
