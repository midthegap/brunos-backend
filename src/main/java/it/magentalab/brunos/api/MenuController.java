package it.magentalab.brunos.api;

import it.magentalab.brunos.service.SocketIoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/menu")
public class MenuController {

	public static final String MENU_IMAGE = "menu.jpg";

	@Value("${menu.upload-dir}")
	private String uploadDir;

	private final SocketIoService socketIoService;

	public MenuController(SocketIoService socketIoService) {
		this.socketIoService = socketIoService;
	}

	@GetMapping("/image")
	public ResponseEntity<Resource> getMenuImage() {
		log.debug("Fetching menu image from: {}", uploadDir);
		try {
			Path imagePath = Paths.get(uploadDir, MENU_IMAGE);
			if (!Files.exists(imagePath)) {
				log.warn("Menu image not found at path: {}", imagePath);
				return ResponseEntity.notFound().build();
			}
			Resource image = new UrlResource(imagePath.toUri());
			String contentType = Files.probeContentType(imagePath);
			if (contentType == null) {
				contentType = "application/octet-stream";
			}

			MediaType mediaType = MediaType.parseMediaType(contentType);
			log.trace("Content type for menu image: {}", mediaType);

			return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getFilename() + "\"")
				.contentType(mediaType)
				.body(image);
		} catch (IOException e) {
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/upload")
	public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("image") MultipartFile file) {
		log.info("Uploading image: {}", file.getOriginalFilename());
		if (file.isEmpty()) {
			var response = buildResponse("File mancante", false);
			return ResponseEntity.badRequest().body(response);
		}
		try {
			Path uploadPath = Paths.get(uploadDir);
			if (!Files.exists(uploadPath)) {
				// Crea la cartella se non esiste
				log.info("Creazione cartella di upload immagini: {}", uploadPath);
				Files.createDirectories(uploadPath);
			}

			Path filePath = uploadPath.resolve(MENU_IMAGE);
			file.transferTo(filePath.toFile());

			socketIoService.menuUpdated();

			var response = buildResponse("Upload riuscito", true);
			return ResponseEntity.ok(response);
		} catch (IOException e) {
			log.warn("Errore upload immagine: {}", e.getMessage());
			var response = buildResponse("Errore upload: " + e.getMessage(), false);
			return ResponseEntity.internalServerError().body(response);
		}
	}

	private Map<String, Object> buildResponse(String message, boolean success) {
		Map<String, Object> response = new HashMap<>();
		response.put("message", message);
		response.put("success", success);
		return response;
	}
}
