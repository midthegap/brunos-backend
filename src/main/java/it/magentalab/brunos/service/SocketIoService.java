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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SocketIoService {
	private static final String ORDER_EVENT = "order";
	private static final String DELETE_EVENT = "delete";
	private static final String RESET_EVENT = "reset";

	private final Set<SocketIOClient> clients = ConcurrentHashMap.newKeySet();

	private final OrderRepository orderRepository;

	@Autowired
	public SocketIoService(SocketIOServer socketIOServer, OrderRepository orderRepository) {
		this.orderRepository = orderRepository;

		socketIOServer.addConnectListener(this.onNewConnection);
		socketIOServer.addDisconnectListener(this.onDisconnection);
	}

	private ConnectListener onNewConnection = client -> {
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
            sendAllOrdersTo(client);
        } else {
            log.debug("Connection upgrade o controllo connettività, client ID: {}", client.getSessionId());
        }
    } else {
        log.warn("Nuova connessione rifiutata. ip={}", ipAddress);
        client.disconnect();
    }
};

	private DisconnectListener onDisconnection = client -> {
		String ipAddress = client.getHandshakeData().getAddress().getAddress().getHostAddress();

		log.info("Device IP {} disconnected. Session ID: {}", ipAddress, client.getSessionId());

		clients.remove(client);
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
		log.info("Send order {}", order);
		clients.forEach(client -> sendOrderTo(client, order));
		log.info("Ordine inviato a tutti i dispositivi");
	}

	public void deleteOrder(Order order) {
		log.info("Delete order {}", order);
		clients.forEach(client -> deleteOrderTo(client, order));
		log.info("Ordine cancellato a tutti i dispositivi");
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
		clients.forEach(client -> reset(client));
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