package com.example.campustrade.category;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.campustrade.domain.Category;
import com.example.campustrade.repository.CategoryRepository;
import com.example.campustrade.repository.ProductRepository;

@Service
public class CategoryService {

	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;

	public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
		this.categoryRepository = categoryRepository;
		this.productRepository = productRepository;
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
	public List<Category> findSelectableCategories() {
		return categoryRepository.findAllByOrderByIdAsc().stream()
				.filter(category -> !category.isRoot())
				.toList();
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

	@Transactional
	public void create(CategoryForm form) {
		if (categoryRepository.existsById(form.getId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリIDが重複しています");
		}
		if (categoryRepository.existsByName(form.getName())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリ名が重複しています");
		}
		Category category = new Category();
		category.setId(form.getId());
		category.setParent(resolveParent(form.getParentId()));
		category.setName(form.getName().trim());
		categoryRepository.save(category);
	}

	@Transactional(readOnly = true)
	public CategoryForm formForEdit(Long id) {
		return CategoryForm.from(findById(id));
	}

	@Transactional
	public void update(Long id, CategoryForm form) {
		Category category = findById(id);
		if (categoryRepository.existsByNameAndIdNot(form.getName(), id)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリ名が重複しています");
		}
		category.setParent(resolveParent(form.getParentId()));
		category.setName(form.getName().trim());
	}

	@Transactional
	public void delete(Long id) {
		Category category = findById(id);
		if (categoryRepository.existsByParentId(id) || productRepository.countByCategoryId(id) > 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "使用中のカテゴリは削除できません");
		}
		categoryRepository.delete(category);
	}

	private Category findById(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	private Category resolveParent(Long parentId) {
		if (parentId == null) {
			return null;
		}
		return categoryRepository.findById(parentId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "親カテゴリが見つかりません"));
	}
}
