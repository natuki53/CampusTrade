package com.example.campustrade.category;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.campustrade.domain.Category;
import com.example.campustrade.repository.CategoryRepository;

@Service
public class CategoryService {

	private final CategoryRepository categoryRepository;

	public CategoryService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	@Transactional(readOnly = true)
	public List<Category> findAll() {
		return categoryRepository.findAllByOrderByIdAsc();
	}

	@Transactional(readOnly = true)
	public List<Category> findRootCategories() {
		return categoryRepository.findByParentIsNullOrderByIdAsc();
	}

	@Transactional(readOnly = true)
	public List<Category> findChildCategories(Long parentId) {
		return categoryRepository.findByParentIdOrderByIdAsc(parentId);
	}

	@Transactional(readOnly = true)
	public List<Long> resolveSearchCategoryIds(Long categoryId) {
		if (categoryId == null) {
			return null;
		}

		return categoryRepository.findById(categoryId)
				.map(category -> {
					if (!category.isRoot()) {
						return List.of(category.getId());
					}
					return categoryRepository.findByParentIdOrderByIdAsc(category.getId())
							.stream()
							.map(Category::getId)
							.toList();
				})
				.orElse(List.of(-1L));
	}
}
