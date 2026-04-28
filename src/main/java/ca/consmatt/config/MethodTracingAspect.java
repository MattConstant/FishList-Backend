package ca.consmatt.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Logs entry/exit for every public method in our controllers, security, and storage layers,
 * along with elapsed time. Enabled by setting {@code app.tracing.enabled=true} (or the env var
 * {@code APP_TRACING_ENABLED=true}); off by default to keep production logs quiet.
 *
 * <pre>
 *   ⤷ entering AuthController.login
 *   ⤴ leaving  AuthController.login (12 ms)
 *
 *   ⤷ entering ImageStorageService.uploadObject
 *   ⤴ leaving  ImageStorageService.uploadObject (failed in 4 ms): IllegalArgumentException
 * </pre>
 */
@Aspect
@Component
@ConditionalOnProperty(name = "app.tracing.enabled", havingValue = "true")
public class MethodTracingAspect {

	private static final Logger log = LoggerFactory.getLogger(MethodTracingAspect.class);

	@Pointcut("within(ca.consmatt.controllers..*) "
			+ "|| within(ca.consmatt.security..*) "
			+ "|| within(ca.consmatt.storage..*) "
			+ "|| within(ca.consmatt.policy..*)")
	public void appPackages() {
	}

	@Pointcut("execution(public * *(..))")
	public void publicMethod() {
	}

	@Around("appPackages() && publicMethod()")
	public Object trace(ProceedingJoinPoint pjp) throws Throwable {
		String method = pjp.getSignature().getDeclaringType().getSimpleName() + "."
				+ pjp.getSignature().getName();
		long start = System.currentTimeMillis();
		log.info("⤷ entering {}", method);
		try {
			Object result = pjp.proceed();
			long ms = System.currentTimeMillis() - start;
			log.info("⤴ leaving  {} ({} ms)", method, ms);
			return result;
		} catch (Throwable ex) {
			long ms = System.currentTimeMillis() - start;
			log.info("⤴ leaving  {} (failed in {} ms): {}", method, ms, ex.getClass().getSimpleName());
			throw ex;
		}
	}
}
