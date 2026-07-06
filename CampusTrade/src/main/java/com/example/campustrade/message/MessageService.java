package com.example.campustrade.message;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.campustrade.domain.AppUser;
import com.example.campustrade.domain.Message;
import com.example.campustrade.domain.MessageType;
import com.example.campustrade.domain.Product;
import com.example.campustrade.domain.TradeStatus;
import com.example.campustrade.domain.UserRole;
import com.example.campustrade.product.ProductService;
import com.example.campustrade.repository.MessageRepository;

@Service
public class MessageService {

	private final MessageRepository messageRepository;
	private final ProductService productService;

	public MessageService(MessageRepository messageRepository, ProductService productService) {
		this.messageRepository = messageRepository;
		this.productService = productService;
	}

	@Transactional(readOnly = true)
	public List<Message> findComments(Long productId) {
		return messageRepository.findCommentsByProductId(productId);
	}

	@Transactional
	public void addComment(Long productId, AppUser sender, MessageForm form) {
		Product product = productService.findViewableProduct(productId, sender);
		if (!product.isVisibleToPublic()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "コメントできない商品です");
		}

		Message message = new Message();
		message.setProduct(product);
		message.setSender(sender);
		message.setReceiver(null);
		message.setMessageType(MessageType.COMMENT);
		message.setContent(form.getContent().trim());
		message.setReadFlag(false);
		messageRepository.save(message);
	}

	@Transactional(readOnly = true)
	public List<Message> findTransactionMessages(Product product, AppUser currentUser) {
		assertTransactionVisible(product, currentUser);
		if (currentUser.getRole() == UserRole.ADMIN) {
			return messageRepository.findTransactionMessagesForAdmin(product.getId());
		}
		return messageRepository.findTransactionThreadForParticipants(
				product.getId(),
				product.getSeller().getId(),
				product.getBuyer().getId());
	}

	@Transactional
	public void addTransactionMessage(Product product, AppUser sender, MessageForm form) {
		assertTransactionVisible(product, sender);
		Message message = new Message();
		message.setProduct(product);
		message.setSender(sender);
		message.setReceiver(resolveTransactionReceiver(product, sender));
		message.setMessageType(MessageType.TRANSACTION);
		message.setContent(form.getContent().trim());
		message.setReadFlag(false);
		messageRepository.save(message);
	}

	private void assertTransactionVisible(Product product, AppUser currentUser) {
		if (product.getTradeStatus() != TradeStatus.LOCKED && product.getTradeStatus() != TradeStatus.CLOSED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "取引詳細を表示できない商品です");
		}
		if (product.getBuyer() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "購入者が未確定です");
		}
		boolean participant = currentUser.getRole() == UserRole.ADMIN
				|| product.getSeller().getId().equals(currentUser.getId())
				|| product.getBuyer().getId().equals(currentUser.getId());
		if (!participant) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
	}

	private AppUser resolveTransactionReceiver(Product product, AppUser sender) {
		if (product.getSeller().getId().equals(sender.getId())) {
			return product.getBuyer();
		}
		if (product.getBuyer().getId().equals(sender.getId())) {
			return product.getSeller();
		}
		return product.getBuyer();
	}
}
