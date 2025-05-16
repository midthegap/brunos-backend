package it.magentalab.brunos.service;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import it.magentalab.brunos.model.Order;
import it.magentalab.brunos.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SocketIoService {
	private static final String ORDER_EVENT = "order";
	private static final String DELETE_EVENT = "delete";
	private static final String RESET_EVENT = "reset";

	private final Map<String, Set<SocketIOClient>> connectedDevices = new ConcurrentHashMap<>();

	private final OrderRepository orderRepository;

	@Autowired
	public SocketIoService(SocketIOServer socketIOServer, OrderRepository orderRepository) {
		this.orderRepository = orderRepository;

		socketIOServer.addConnectListener(this.onNewConnection);
		socketIOServer.addDisconnectListener(this.onDisconnection);
	}

	private DisconnectListener onDisconnection = client -> {
		String ipAddress = client.getHandshakeData().getAddress().getAddress().getHostAddress();

		Set<SocketIOClient> sessions = connectedDevices.get(ipAddress);
		if (sessions != null) {
			sessions.remove(client);
			if (sessions.isEmpty()) {
				connectedDevices.remove(ipAddress);
				log.info("WS: Connessione chiusa, ip disconnesso={}", ipAddress);
			} else {
				log.debug("WS: Connessione chiusa. ip={} ws_id={}. Sessioni rimaste={}", ipAddress, client.getSessionId(), sessions.size());
			}
		}
	};

	private ConnectListener onNewConnection = client -> {
		String ipAddress = client.getHandshakeData().getAddress().getAddress().getHostAddress();

		log.info("New client connection from {}", ipAddress);

		if (ipAddress != null) {
			boolean isNewConnection = !connectedDevices.containsKey(ipAddress);
			connectedDevices.computeIfAbsent(ipAddress, k -> ConcurrentHashMap.newKeySet()).add(client);
			log.info("WS: Device connesso con ip={}. Numero di sessioni={}", ipAddress, connectedDevices.get(ipAddress).size());

			if (isNewConnection) {
				sendAllOrdersTo(client);
			}
		} else {
			log.warn("WS: Nuova connessione rifiutata. ip={}", ipAddress);
			client.disconnect();
		}
	};

	private void sendAllOrdersTo(SocketIOClient client) {
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
		log.info("WS: Invio ordine {}", order);
		connectedDevices.forEach((ip, sessions) -> {
			sessions.forEach(client -> sendOrderTo(client, order));
		});
		log.info("WS: Ordine inviato a tutti i dispositivi");
	}

	public void deleteOrder(Order order) {
		log.info("WS: Cancella ordine {}", order);
		connectedDevices.forEach((ip, sessions) -> {
			sessions.forEach(client -> deleteOrderTo(client, order));
		});
		log.info("WS: Ordine cancellato a tutti i dispositivi");
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
		connectedDevices.forEach((ip, sessions) -> {
			sessions.forEach(client -> reset(client));
		});
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
}
