package com.example.campustrade.product;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.campustrade.category.CategoryService;
import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.Category;
import com.example.campustrade.domain.ModerationStatus;
import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.ProductImage;
import com.example.campustrade.domain.TradeStatus;
import com.example.campustrade.domain.UserRole;
import com.example.campustrade.repository.AppUserRepository;
import com.example.campustrade.repository.CategoryRepository;
import com.example.campustrade.repository.ProductImageRepository;
import com.example.campustrade.repository.ProductRepository;

@Service
public class ProductService {

	private static final int MAX_IMAGE_COUNT = 5;
	private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
	private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

	private final ProductRepository productRepository;
	private final CategoryService categoryService;
	private final CategoryRepository categoryRepository;
	private final AppUserRepository appUserRepository;
	private final ProductImageRepository productImageRepository;

	public ProductService(ProductRepository productRepository, CategoryService categoryService,
			CategoryRepository categoryRepository, AppUserRepository appUserRepository,
			ProductImageRepository productImageRepository) {
		this.productRepository = productRepository;
		this.categoryService = categoryService;
		this.categoryRepository = categoryRepository;
		this.appUserRepository = appUserRepository;
		this.productImageRepository = productImageRepository;
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
	public Map<Long, Long> findPrimaryImageIds(List<Product> products) {
		Map<Long, Long> primaryImageIds = new HashMap<>();
		for (Product product : products) {
			productImageRepository.findFirstByProductIdAndPrimaryFlagTrueOrderByDisplayOrderAsc(product.getId())
					.ifPresent(image -> primaryImageIds.put(product.getId(), image.getId()));
		}
		return primaryImageIds;
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

	@Transactional
	public Product createProduct(ProductForm form, AppUser currentUser) {
		AppUser seller = appUserRepository.getReferenceById(currentUser.getId());
		Category category = findSelectableCategory(form.getCategoryId());
		List<MultipartFile> images = validatedImages(form.getImages());

		Product product = new Product();
		applyForm(product, form, category);
		product.setSeller(seller);
		product.setBuyer(null);
		product.setTradeStatus(TradeStatus.OPEN);
		product.setModerationStatus(ModerationStatus.ACTIVE);
		product.setDeletedAt(null);
		Product saved = productRepository.save(product);
		saveImages(saved, images);
		return saved;
	}

	@Transactional(readOnly = true)
	public Product findEditableProduct(Long id, AppUser currentUser) {
		Product product = productRepository.findDetailById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (!product.getSeller().getId().equals(currentUser.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		if (product.getTradeStatus() != TradeStatus.OPEN) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "取引中の商品は編集できません");
		}
		return product;
	}

	@Transactional(readOnly = true)
	public List<Product> findPurchases(AppUser currentUser) {
		return productRepository.findPurchasesForBuyer(currentUser.getId(), List.of(TradeStatus.LOCKED, TradeStatus.CLOSED));
	}

	@Transactional(readOnly = true)
	public List<Product> findSales(AppUser currentUser) {
		return productRepository.findSalesForSeller(currentUser.getId());
	}

	@Transactional(readOnly = true)
	public List<Product> findAllForAdmin() {
		return productRepository.findAllForAdmin();
	}

	@Transactional
	public void prohibit(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		product.setModerationStatus(ModerationStatus.PROHIBITED);
	}

	@Transactional
	public void activate(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		product.setModerationStatus(ModerationStatus.ACTIVE);
	}

	@Transactional
	public Product updateProduct(Long id, ProductForm form, AppUser currentUser) {
		Product product = findEditableProduct(id, currentUser);
		Category category = findSelectableCategory(form.getCategoryId());
		List<MultipartFile> newImages = validatedImages(form.getImages());
		List<ProductImage> existingImages = productImageRepository.findByProductIdOrderByDisplayOrderAsc(
				product.getId());
		Set<Long> removeImageIds = normalizedRemoveImageIds(form.getRemoveImageIds());
		List<ProductImage> imagesToRemove = existingImages.stream()
				.filter(image -> removeImageIds.contains(image.getId()))
				.toList();
		if (imagesToRemove.size() != removeImageIds.size()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "削除対象の画像が正しくありません");
		}
		List<ProductImage> remainingImages = existingImages.stream()
				.filter(image -> !removeImageIds.contains(image.getId()))
				.toList();
		if (remainingImages.size() + newImages.size() > MAX_IMAGE_COUNT) {
			throw new InvalidProductImageException("画像は最大5枚まで登録できます");
		}

		applyForm(product, form, category);
		if (!imagesToRemove.isEmpty()) {
			productImageRepository.deleteAll(imagesToRemove);
		}
		renumberImages(remainingImages);
		saveImages(product, newImages, remainingImages.size());
		return product;
	}

	@Transactional
	public void deleteProductImage(Long productId, Long imageId, AppUser currentUser) {
		Product product = findEditableProduct(productId, currentUser);
		List<ProductImage> existingImages = productImageRepository.findByProductIdOrderByDisplayOrderAsc(
				product.getId());
		ProductImage imageToRemove = existingImages.stream()
				.filter(image -> image.getId().equals(imageId))
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		List<ProductImage> remainingImages = existingImages.stream()
				.filter(image -> !image.getId().equals(imageToRemove.getId()))
				.toList();

		productImageRepository.delete(imageToRemove);
		renumberImages(remainingImages);
	}

	@Transactional
	public void softDelete(Long id, AppUser currentUser) {
		Product product = productRepository.findDetailById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (!product.getSeller().getId().equals(currentUser.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		if (product.getTradeStatus() != TradeStatus.OPEN) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "取引中または取引完了の商品は非公開にできません");
		}
		product.setDeletedAt(LocalDateTime.now());
	}

	@Transactional
	public void publish(Long id, AppUser currentUser) {
		Product product = productRepository.findDetailById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (!product.getSeller().getId().equals(currentUser.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		if (product.getTradeStatus() != TradeStatus.OPEN) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "取引中または取引完了の商品は公開できません");
		}
		if (product.getModerationStatus() == ModerationStatus.PROHIBITED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理者により非表示の商品は公開できません");
		}
		product.setDeletedAt(null);
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

	private Category findSelectableCategory(Long categoryId) {
		Category category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリを選択してください"));
		if (category.isRoot()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "小分類を選択してください");
		}
		return category;
	}

	private void applyForm(Product product, ProductForm form, Category category) {
		product.setName(form.getName().trim());
		product.setDescription(form.getDescription().trim());
		product.setPrice(form.getPrice());
		product.setCategory(category);
		product.setConditionLabel(form.getConditionLabel().trim());
	}

	private List<MultipartFile> validatedImages(List<MultipartFile> files) {
		if (files == null) {
			return List.of();
		}
		List<MultipartFile> images = files.stream()
				.filter(file -> file != null && !file.isEmpty())
				.toList();
		if (images.size() > MAX_IMAGE_COUNT) {
			throw new InvalidProductImageException("画像は最大5枚まで登録できます");
		}
		for (MultipartFile image : images) {
			if (image.getSize() > MAX_IMAGE_SIZE) {
				throw new InvalidProductImageException("画像は1枚5MBまでです");
			}
			if (!ALLOWED_IMAGE_TYPES.contains(image.getContentType())) {
				throw new InvalidProductImageException("画像はJPEG、PNG、WebPのみ登録できます");
			}
		}
		return images;
	}

	private void saveImages(Product product, List<MultipartFile> images) {
		saveImages(product, images, 0);
	}

	private void saveImages(Product product, List<MultipartFile> images, int startDisplayOrder) {
		for (int i = 0; i < images.size(); i++) {
			MultipartFile multipartFile = images.get(i);
			ProductImage image = new ProductImage();
			image.setProduct(product);
			image.setOriginalFilename(multipartFile.getOriginalFilename() == null ? "image" : multipartFile.getOriginalFilename());
			image.setContentType(multipartFile.getContentType());
			image.setDisplayOrder(startDisplayOrder + i);
			image.setPrimaryFlag(startDisplayOrder + i == 0);
			try {
				image.setImageData(multipartFile.getBytes());
			} catch (IOException ex) {
				throw new InvalidProductImageException("画像を読み込めませんでした");
			}
			productImageRepository.save(image);
		}
	}

	private Set<Long> normalizedRemoveImageIds(List<Long> removeImageIds) {
		if (removeImageIds == null) {
			return Set.of();
		}
		return removeImageIds.stream()
				.filter(id -> id != null)
				.collect(Collectors.toSet());
	}

	private void renumberImages(List<ProductImage> images) {
		for (int i = 0; i < images.size(); i++) {
			ProductImage image = images.get(i);
			image.setDisplayOrder(i);
			image.setPrimaryFlag(i == 0);
		}
	}
}
