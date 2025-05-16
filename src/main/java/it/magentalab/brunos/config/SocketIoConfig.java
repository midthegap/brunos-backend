package it.magentalab.brunos.config;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin
@Configuration
@Slf4j
public class SocketIoConfig {
	private SocketIOServer server;

	@Value("${socket.io.port:9090}")
	private int port;

	@Value("${socket.io.host:localhost}")
	private String host;

	@Bean
	public SocketIOServer socketIOServer() {
		com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
		config.setHostname(host);
		config.setPort(port);

		// configurazione CORS per Socket.IO
		config.setOrigin("*");  // Permetti richieste da qualsiasi origine
		// config.setOrigin("http://localhost:4200"); // per Angular in sviluppo locale

		this.server = new SocketIOServer(config);
		this.server.start();

		log.info("Created socket connection on port={} host={}", port, host);
		return server;
	}

	@PreDestroy
	public void destroy(){
		log.info("Destroying socket connection");
		this.server.stop();
	}
}