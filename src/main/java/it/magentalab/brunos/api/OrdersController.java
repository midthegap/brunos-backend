package it.magentalab.brunos.api;

import it.magentalab.brunos.dto.OrderDto;
import it.magentalab.brunos.model.Order;
import it.magentalab.brunos.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controller che accetta le richieste senza verificare l'ip.
 */
@RestController
@RequestMapping("/api/order")
@Slf4j
public class OrdersController {

	@Value("${build.version}")
	private String version;

	@Autowired
	private OrderService orderService;

	@PostMapping("")
	public ResponseEntity<?> save(@RequestBody OrderDto orderDto) {
		orderService.save(orderDto);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("")
	public ResponseEntity<?> delete(@RequestBody OrderDto orderDto) {
		log.info("Deleting order: {}", orderDto);
		orderService.delete(orderDto);
		return ResponseEntity.ok().build();
	}

	/**
	 * Generates a text report of all orders in the system.
	 *
	 * @return A plain text report of all orders
	 */
	@GetMapping(value = "/report", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> generateReport() {
		log.info("Generating orders report");
		String report = orderService.generateOrdersReport();
		return ResponseEntity.ok(report);
	}

	/**
	 * Clears all orders from the repository.
	 *
	 * @return Response with HTTP status
	 */
	@DeleteMapping("/all")
	public ResponseEntity<?> clearAllOrders() {
		log.info("Clearing all orders from repository");
		int deletedCount = orderService.deleteAllOrders();
		log.info("Deleted {} orders", deletedCount);
		return ResponseEntity.ok().build();
	}

	/**
	 * Retrieves all orders from the repository.
	 *
	 * @return A list of all orders
	 */
	@GetMapping("")
	public ResponseEntity<List<Order>> getAllOrders() {
		log.info("Retrieving all orders");
		List<Order> orders = orderService.findAll();
		return ResponseEntity.ok(orders);
	}
}