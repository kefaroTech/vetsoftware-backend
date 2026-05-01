package com.vetsoftware.app.infrastructure.config;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class ObservabilityConfig {

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry registry) {
        return new ObservedAspect(registry);
    }

    @Aspect
    @Component
    public static class UseCaseObservationAspect {

        private final ObservationRegistry registry;

        public UseCaseObservationAspect(ObservationRegistry registry) {
            this.registry = registry;
        }

        @Around("execution(* com.vetsoftware.app..application.usecase..*(..))")
        public Object observe(ProceedingJoinPoint pjp) throws Throwable {
            String className = pjp.getTarget().getClass().getSimpleName();
            String methodName = pjp.getSignature().getName();
            Observation.CheckedCallable<Object, Throwable> callable = pjp::proceed;
            return Observation.createNotStarted("usecase", registry)
                    .lowCardinalityKeyValue("class", className)
                    .lowCardinalityKeyValue("method", methodName)
                    .observeChecked(callable);
        }
    }
}
