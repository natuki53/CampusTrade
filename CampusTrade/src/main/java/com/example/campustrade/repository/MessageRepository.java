package com.example.campustrade.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.campustrade.domain.Message;
import com.example.campustrade.domain.MessageType;

public interface MessageRepository extends JpaRepository<Message, Long> {

	List<Message> findByProductIdAndMessageTypeOrderByCreatedAtAsc(Long productId, MessageType messageType);

	@Query("""
			select m from Message m
			join fetch m.sender
			where m.product.id = :productId
			  and m.messageType = com.example.campustrade.domain.MessageType.COMMENT
			order by m.createdAt asc
			""")
	List<Message> findCommentsByProductId(@Param("productId") Long productId);

	@Query("""
			select m from Message m
			join fetch m.sender
			left join fetch m.receiver
			where m.product.id = :productId
			  and m.messageType = com.example.campustrade.domain.MessageType.TRANSACTION
			  and (
			    (m.sender.id = :sellerId and m.receiver.id = :buyerId)
			    or (m.sender.id = :buyerId and m.receiver.id = :sellerId)
			    or m.sender.role = com.example.campustrade.domain.UserRole.ADMIN
			    or m.receiver.role = com.example.campustrade.domain.UserRole.ADMIN
			  )
			order by m.createdAt asc
			""")
	List<Message> findTransactionThreadForParticipants(
			@Param("productId") Long productId,
			@Param("sellerId") Long sellerId,
			@Param("buyerId") Long buyerId);
}
