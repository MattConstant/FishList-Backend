package ca.consmatt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the FishList Spring Boot application.
 */
@SpringBootApplication
public class FishListApplication {

	/**
	 * Starts the embedded web server and Spring context.
	 *
	 * @param args standard command-line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(FishListApplication.class, args);
	}

}
