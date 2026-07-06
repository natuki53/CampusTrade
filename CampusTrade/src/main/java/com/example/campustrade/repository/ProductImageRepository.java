package com.example.campustrade.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campustrade.domain.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

	List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);

	Optional<ProductImage> findByProductIdAndId(Long productId, Long id);

	Optional<ProductImage> findFirstByProductIdAndPrimaryFlagTrueOrderByDisplayOrderAsc(Long productId);

	void deleteByProductId(Long productId);
}
