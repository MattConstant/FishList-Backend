package ca.consmatt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI metadata and security scheme for Swagger UI (Bearer JWT).
 * Not loaded when {@code prod} profile is active (see {@code application-prod.properties}).
 */
@Configuration
@Profile("!prod")
public class OpenApiConfiguration {

	/**
	 * @return API title, description, version, and {@code bearerAuth} scheme
	 */
	@Bean
	public OpenAPI fishListOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("FishList API")
						.description("Fishing spot tracker: accounts, locations, catches, fish species, and conditions.")
						.version("1.0.0"))
				.components(new Components()
						.addSecuritySchemes("bearerAuth", new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("FishList JWT from POST /api/auth/google (after Google Sign-In)")));
	}
}
