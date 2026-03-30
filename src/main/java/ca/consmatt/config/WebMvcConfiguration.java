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

	public WebMvcConfiguration(ApiRequestLoggingInterceptor apiRequestLoggingInterceptor) {
		this.apiRequestLoggingInterceptor = apiRequestLoggingInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(apiRequestLoggingInterceptor).addPathPatterns("/api/**");
	}
}
