package ca.consmatt.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers shared MVC behavior for API endpoints.
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

	private final ApiRequestLoggingInterceptor apiRequestLoggingInterceptor;
	private final ApiRateLimitInterceptor apiRateLimitInterceptor;

	public WebMvcConfiguration(
			ApiRequestLoggingInterceptor apiRequestLoggingInterceptor,
			ApiRateLimitInterceptor apiRateLimitInterceptor) {
		this.apiRequestLoggingInterceptor = apiRequestLoggingInterceptor;
		this.apiRateLimitInterceptor = apiRateLimitInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(apiRequestLoggingInterceptor).addPathPatterns("/api/**");
		registry.addInterceptor(apiRateLimitInterceptor).addPathPatterns("/api/**");
	}
}
