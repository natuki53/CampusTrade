package com.example.campustrade.sample;

import java.io.IOException;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.ProductImage;
import com.example.campustrade.repository.ProductImageRepository;
import com.example.campustrade.repository.ProductRepository;

@Component
public class SampleImageDataInitializer implements ApplicationRunner {

	private static final Map<String, String> SAMPLE_IMAGES = Map.ofEntries(
			Map.entry("統計学入門 第3版", "textbook.jpg"),
			Map.entry("TOEIC対策 公式問題集", "notebook.jpg"),
			Map.entry("文房具セット", "stationery.jpg"),
			Map.entry("MacBook Air 13インチ", "laptop.jpg"),
			Map.entry("電気ケトル 0.8L", "kettle.jpg"),
			Map.entry("キッチン用品セット", "kitchen.jpg"),
			Map.entry("収納ボックス 3個セット", "storage.jpg"),
			Map.entry("マンガ全巻セット", "books.jpg"),
			Map.entry("ゲームコントローラー", "controller.jpg"),
			Map.entry("ハンドメイドキーホルダー", "beads.jpg"),
			Map.entry("デスクライト", "desk-lamp.jpg"),
			Map.entry("就活用バッグ", "backpack.jpg"));

	private final ProductRepository productRepository;
	private final ProductImageRepository productImageRepository;
	private final ResourceLoader resourceLoader;

	public SampleImageDataInitializer(ProductRepository productRepository,
			ProductImageRepository productImageRepository,
			ResourceLoader resourceLoader) {
		this.productRepository = productRepository;
		this.productImageRepository = productImageRepository;
		this.resourceLoader = resourceLoader;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		SAMPLE_IMAGES.forEach(this::insertImageIfMissing);
	}

	private void insertImageIfMissing(String productName, String imageFileName) {
		productRepository.findByName(productName).ifPresent(product -> {
			if (productImageRepository.findFirstByProductIdAndPrimaryFlagTrueOrderByDisplayOrderAsc(product.getId()).isPresent()) {
				return;
			}
			ProductImage image = new ProductImage();
			image.setProduct(product);
			image.setOriginalFilename(imageFileName);
			image.setContentType("image/jpeg");
			image.setDisplayOrder(0);
			image.setPrimaryFlag(true);
			image.setImageData(readImage(imageFileName));
			productImageRepository.save(image);
		});
	}

	private byte[] readImage(String imageFileName) {
		Resource resource = resourceLoader.getResource("classpath:sample-images/" + imageFileName);
		try {
			return resource.getInputStream().readAllBytes();
		} catch (IOException ex) {
			throw new IllegalStateException("サンプル画像を読み込めません: " + imageFileName, ex);
		}
	}
}
