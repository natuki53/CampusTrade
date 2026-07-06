package com.example.campustrade.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.ModerationStatus;
import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.TradeStatus;

public interface ProductRepository extends JpaRepository<Product, Long> {

	@Query("""
			select p from Product p
			join fetch p.seller
			join fetch p.category c
			left join fetch c.parent
			where p.tradeStatus = com.example.campustrade.domain.TradeStatus.OPEN
			  and p.moderationStatus = com.example.campustrade.domain.ModerationStatus.ACTIVE
			  and p.deletedAt is null
			  and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%'))
			    or lower(p.description) like lower(concat('%', :keyword, '%')))
			  and (:categoryIds is null or p.category.id in :categoryIds)
			order by p.createdAt desc
			""")
	List<Product> searchPublicProducts(@Param("keyword") String keyword, @Param("categoryIds") Collection<Long> categoryIds);

	List<Product> findBySellerIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long sellerId);

	List<Product> findByBuyerIdAndTradeStatusInOrderByCreatedAtDesc(Long buyerId, Collection<TradeStatus> tradeStatuses);

	List<Product> findAllByOrderByCreatedAtDesc();

	long countByCategoryId(Long categoryId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update Product p
			set p.buyer = :buyer,
			    p.tradeStatus = com.example.campustrade.domain.TradeStatus.LOCKED
			where p.id = :productId
			  and p.tradeStatus = com.example.campustrade.domain.TradeStatus.OPEN
			  and p.moderationStatus = com.example.campustrade.domain.ModerationStatus.ACTIVE
			  and p.deletedAt is null
			  and p.seller.id <> :buyerId
			""")
	int lockForPurchase(@Param("productId") Long productId, @Param("buyer") AppUser buyer, @Param("buyerId") Long buyerId);

	List<Product> findByModerationStatusOrderByCreatedAtDesc(ModerationStatus moderationStatus);
}
