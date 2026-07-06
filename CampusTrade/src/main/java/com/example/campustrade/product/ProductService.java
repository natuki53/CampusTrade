package com.example.campustrade.product;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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
		List<MultipartFile> images = validatedImages(form.getImages());

		applyForm(product, form, category);
		if (!images.isEmpty()) {
			productImageRepository.deleteByProductId(product.getId());
			saveImages(product, images);
		}
		return product;
	}

	@Transactional
	public void softDelete(Long id, AppUser currentUser) {
		Product product = productRepository.findDetailById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (!product.getSeller().getId().equals(currentUser.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		product.setDeletedAt(LocalDateTime.now());
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
		for (int i = 0; i < images.size(); i++) {
			MultipartFile multipartFile = images.get(i);
			ProductImage image = new ProductImage();
			image.setProduct(product);
			image.setOriginalFilename(multipartFile.getOriginalFilename() == null ? "image" : multipartFile.getOriginalFilename());
			image.setContentType(multipartFile.getContentType());
			image.setDisplayOrder(i);
			image.setPrimaryFlag(i == 0);
			try {
				image.setImageData(multipartFile.getBytes());
			} catch (IOException ex) {
				throw new InvalidProductImageException("画像を読み込めませんでした");
			}
			productImageRepository.save(image);
		}
	}
}
