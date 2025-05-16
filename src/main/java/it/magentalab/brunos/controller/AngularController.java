package it.magentalab.brunos.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
public class AngularController implements ErrorController {

	@RequestMapping(value = "${server.error.path:${error.path:/error}}")
	public String error(HttpServletRequest request) {
		log.debug("moving to /index.html, requested: {}", request.getRequestURI());
		return "forward:/index.html";
	}
}