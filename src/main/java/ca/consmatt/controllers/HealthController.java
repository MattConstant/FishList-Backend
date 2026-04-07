package ca.consmatt.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal liveness endpoint with no JPA or other heavy dependencies — faster to initialize when
 * {@code spring.main.lazy-initialization=true} and cheap for platform health checks (e.g. Render).
 */
@RestController
public class HealthController {

	@GetMapping({ "/api/health", "/api/healthz" })
	public String health() {
		return "ok";
	}
}
