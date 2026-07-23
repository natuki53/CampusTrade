package com.example.campustrade.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campustrade.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	List<Category> findAllByOrderByIdAsc();

	List<Category> findByParentIsNullOrderByIdAsc();

	List<Category> findByParentIdOrderByIdAsc(Long parentId);

	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, Long id);

	boolean existsByParentId(Long parentId);
}
