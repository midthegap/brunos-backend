package it.magentalab.brunos.service;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import it.magentalab.brunos.dto.PostMessage;
import it.magentalab.brunos.model.Order;
import it.magentalab.brunos.repository.OrderRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SocketIoService {
	private static final String ORDER_EVENT = "order";
	private static final String DELETE_EVENT = "delete";
	private static final String RESET_EVENT = "reset";
	private static final String POST_EVENT = "post";
	private static final String INIT_EVENT = "init";
	private static final String MENU_UPDATED_EVENT = "menu-updated";

	private final Set<SocketIOClient> clients = ConcurrentHashMap.newKeySet();
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	private final OrderRepository orderRepository;
	private final SocketIOServer socketIOServer;

	@Autowired
	public SocketIoService(SocketIOServer socketIOServer, OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
		this.socketIOServer = socketIOServer;

		socketIOServer.addConnectListener(this.onNewConnect);
		socketIOServer.addDisconnectListener(this.onDisconnect);
		socketIOServer.addEventListener(POST_EVENT, PostMessage.class, this::onPostEventReceived);
	}

	private final ConnectListener onNewConnect = client -> {
		String ipAddress = client.getHandshakeData().getAddress().getAddress().getHostAddress();
		String transport = client.getHandshakeData().getHttpHeaders().get("Connection");
		String upgrade = client.getHandshakeData().getHttpHeaders().get("Upgrade");

		log.debug("Connection request: IP={}, Transport={}, Upgrade={}", ipAddress, transport, upgrade);

		if (ipAddress != null) {
			// Verifica se il client è già connesso
			boolean clientExists = clients.stream()
				.anyMatch(c -> c.getSessionId().equals(client.getSessionId()));

			if (!clientExists) {
				clients.add(client);
				log.info("Connessione accettata, client ID: {}", client.getSessionId());
				sendInit(client);
				scheduler.schedule(() -> sendAllOrdersTo(client), 1500, TimeUnit.MILLISECONDS);
			} else {
				log.debug("Connection upgrade o controllo connettività, client ID: {}", client.getSessionId());
			}
		} else {
			log.warn("Nuova connessione rifiutata. ip={}", ipAddress);
			client.disconnect();
		}
	};

	private final DisconnectListener onDisconnect = client -> {
		String ipAddress = client.getHandshakeData().getAddress().getAddress().getHostAddress();

		log.info("Device IP {} disconnected. Session ID: {}", ipAddress, client.getSessionId());

		clients.remove(client);
	};

	private void onPostEventReceived(SocketIOClient sender, PostMessage message, AckRequest ackRequest) {
		log.info("Post dal client {}: {}", sender.getSessionId(), message);
		// Inoltra a tutti tranne il mittente
		clients.stream()
			.filter(client -> !client.getSessionId().equals(sender.getSessionId()))
			.forEach(client -> client.sendEvent(POST_EVENT, message));
	}

	private void sendInit(SocketIOClient client) {
		log.debug("Sending init event to client {}", client.getSessionId());
		client.sendEvent(INIT_EVENT);
	}

	private void sendAllOrdersTo(SocketIOClient client) {
		log.debug("Sending all orders to client {}", client.getSessionId());
		orderRepository.findAll().forEach(order -> {
			sendOrderTo(client, order);
		});
	}

	private void sendOrderTo(SocketIOClient client, Order order) {
		try {
			log.trace("Sending {} to client {}...", order, client.getSessionId());
			client.sendEvent(ORDER_EVENT, order);
		} catch (Exception e) {
			log.error("WS: Errore nella comunicazione session id={}", client.getSessionId());
		}
	}

	public void sendOrder(Order order) {
		log.info("Send order {}", order);
		clients.forEach(client -> sendOrderTo(client, order));
		log.info("Ordine inviato a tutti i dispositivi");
	}

	public void deleteOrder(Order order) {
		log.info("Delete order {}", order);
		clients.forEach(client -> deleteOrderTo(client, order));
		log.info("Ordine cancellato a tutti i dispositivi");
	}

	public void menuUpdated() {
		log.info("Send '{}' to all clients", MENU_UPDATED_EVENT);
		clients.forEach(client -> client.sendEvent(MENU_UPDATED_EVENT));
	}

	private void deleteOrderTo(SocketIOClient client, Order order) {
		try {
			log.trace("Deleting {} to client {}...", order, client.getSessionId());
			client.sendEvent(DELETE_EVENT, order);
		} catch (Exception e) {
			log.error("WS: errore nella comunicazione session id={}", client.getSessionId());
		}
	}

	public void reset() {
		log.info("WS: reset...");
		clients.forEach(this::reset);
		log.info("WS: reset done");
	}

	private void reset(SocketIOClient client) {
		try {
			log.trace("Reset client {}...", client.getSessionId());
			client.sendEvent(RESET_EVENT);
		} catch (Exception e) {
			log.error("WS: errore nella comunicazione session id={}", client.getSessionId());
		}
	}

	@PreDestroy
	public void shutdownSocketServer() {
		if (this.socketIOServer != null) {
			log.info("Arresto SocketIO...");
			this.socketIOServer.stop();
		}
	}
}