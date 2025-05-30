package it.magentalab.brunos.service;

import it.magentalab.brunos.dto.OrderDto;
import it.magentalab.brunos.model.Order;
import it.magentalab.brunos.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderService {

	private final OrderRepository orderRepository;
	private final SocketIoService socketIoService;

	@Autowired
	public OrderService(OrderRepository orderRepository, SocketIoService socketIoService) {
		this.orderRepository = orderRepository;
		this.socketIoService = socketIoService;
	}

	public Order save(OrderDto orderDto) {
		// check, maybe there's already an order with the same name and article
		List<Order> existingOrders = orderRepository.findByNameAndArticle(orderDto.name(), orderDto.article());
		if (!existingOrders.isEmpty()) {
			var existingOrder = existingOrders.get(0);
			log.warn("Order already exists: {}", existingOrder);
			return existingOrder;
		}

		// Convert OrderDto to Order entity
		Order order = new Order();
		order.setName(orderDto.name());
		order.setArticle(orderDto.article());

		var saved = orderRepository.save(order);
		this.socketIoService.sendOrder(saved);
		return saved;
	}

	public void delete(OrderDto orderDto) {
		orderRepository.findByNameAndArticle(orderDto.name(), orderDto.article())
			.forEach(order -> {
				log.info("Deleting order: {}", order);
				orderRepository.delete(order);
				this.socketIoService.deleteOrder(order);
			});
	}

	public String generateReport() {
		List<Order> allOrders = orderRepository.findAll();
		StringBuilder report = new StringBuilder();

		if (allOrders.isEmpty()) {
			report.append("No orders found in the system.\n");
		} else {
			var guestsNumber = allOrders.size();
			report.append("Ciao Bruno, oggi ");
			if (guestsNumber == 1) {
				report.append("ci sono solo io");
			} else {
				report.append("siamo in ").append(guestsNumber);
			}
			report.append(":\n");

			// Group orders by article and count them
			Map<String, Long> articleCounts = allOrders.stream()
				.collect(Collectors.groupingBy(Order::getArticle, Collectors.counting()));

			// Sort articles by count in descending order
			articleCounts.entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByValue().reversed())
				.forEach(entry -> {
					String article = entry.getKey();
					Long count = entry.getValue();
					report.append(count)
						.append(" x ")
						.append(article)
						.append("\n");
				});
		}

		//report.append("\nReport generated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

		return report.toString();
	}

	/**
	 * Deletes all orders from the repository.
	 *
	 * @return Number of deleted orders
	 */
	public int deleteAllOrders() {
		long count = orderRepository.count();
		orderRepository.deleteAll();
		this.socketIoService.reset();
		return (int) count;
	}

	public List<Order> findAll() {
		return orderRepository.findAll();
	}
}