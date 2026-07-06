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
}
