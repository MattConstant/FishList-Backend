package ca.consmatt.security;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import io.jsonwebtoken.security.Keys;

/**
 * JWT bearer authentication (FishList-issued tokens), CORS, CSRF off for APIs.
 */
@Configuration
@EnableWebSecurity
@org.springframework.boot.context.properties.EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter)
			throws Exception {
		http
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.cors(Customizer.withDefaults())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/api/health", "/api/healthz").permitAll()
						.requestMatchers("/api/locations/heartbeat", "/api/locations/healthcheck").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/google").permitAll()
						.requestMatchers("/h2-console/**").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
				.csrf(csrf -> csrf.disable())
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
		return http.build();
	}

	@Bean
	public JwtDecoder jwtDecoder(FishListJwtProperties props) {
		if (props.getSecret() == null || props.getSecret().length() < 32) {
			throw new IllegalStateException("app.jwt.secret must be at least 32 characters; set JWT_SECRET");
		}
		SecretKey key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
		return NimbusJwtDecoder.withSecretKey(key).build();
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt -> Collections.emptyList());
		converter.setPrincipalClaimName("username");
		return converter;
	}

	/**
	 * CORS for cross-origin frontends. Spring Security picks up this {@link CorsConfigurationSource}
	 * bean when {@code http.cors(...)} is enabled on the filter chain.
	 *
	 * @param corsProperties bound from {@code app.cors.*}
	 * @return source registered for all paths
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
		if (corsProperties.isAllowCredentials()) {
			for (String pattern : corsProperties.getAllowedOriginPatterns()) {
				if ("*".equals(pattern)) {
					throw new IllegalStateException(
							"app.cors.allow-credentials=true is incompatible with app.cors.allowed-origin-patterns=*; "
									+ "list explicit origins (e.g. http://localhost:5173)");
				}
			}
		}
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());
		config.setAllowedMethods(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setExposedHeaders(List.of("Location", "Content-Disposition"));
		config.setMaxAge(corsProperties.getMaxAgeSeconds());
		config.setAllowCredentials(corsProperties.isAllowCredentials());
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
