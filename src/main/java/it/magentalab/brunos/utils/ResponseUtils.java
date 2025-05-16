package it.magentalab.brunos.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseUtils {
	public static ResponseEntity<?> unauthorized() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	}
	public static <T> ResponseEntity<?> unauthorized(T body) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
	}

	public static ResponseEntity<?> bool2Response(boolean success) {
		return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
	}

	public static ResponseEntity<?> bool2Response(boolean success, String okMessage, String errorMessage) {
		return success ? ResponseEntity.ok(okMessage) : ResponseEntity.badRequest().body(errorMessage);
	}

	public static ResponseEntity<?> badRequest(String message) {
		return ResponseEntity.badRequest().body(message);
	}
	public static ResponseEntity<?> badRequest() {
		return ResponseEntity.badRequest().build();
	}

	public static <T>ResponseEntity<?> ok(T body) {
		return ResponseEntity.ok(body);
	}
}
