package com.vetsoftware.app.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.ObservationView;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.observation.aop.ObservedAspect;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncObservedIntegrationTest {

    @Test
    void createsTheObservedChildInsideTheWorkerAndKeepsItsParent() throws Exception {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(TestConfiguration.class)) {
            ObservationRegistry registry = context.getBean(ObservationRegistry.class);
            AsyncObservedWorker worker = context.getBean(AsyncObservedWorker.class);
            Observation parent = Observation.start("test.parent", registry);
            AsyncState state;

            try (Observation.Scope ignored = parent.openScope()) {
                state = worker.execute().get(5, TimeUnit.SECONDS);
            } finally {
                parent.stop();
            }

            assertThat(state.threadName()).startsWith("email-");
            assertThat(state.observationName()).isEqualTo("test.async");
            assertThat(state.parent()).isSameAs(parent);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAsync
    @EnableAspectJAutoProxy
    static class TestConfiguration {

        @Bean
        ObservationRegistry observationRegistry() {
            ObservationRegistry registry = ObservationRegistry.create();
            registry.observationConfig().observationHandler(
                    new ObservationHandler<Observation.Context>() {
                        @Override
                        public boolean supportsContext(Observation.Context context) {
                            return true;
                        }
                    });
            return registry;
        }

        @Bean
        ObservedAspect observedAspect(ObservationRegistry registry) {
            return new ObservedAspect(registry);
        }

        @Bean
        TaskDecorator contextPropagatingTaskDecorator(ObservationRegistry registry) {
            ContextRegistry contextRegistry = new ContextRegistry()
                    .registerThreadLocalAccessor(new ObservationThreadLocalAccessor(registry));
            ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
                    .contextRegistry(contextRegistry)
                    .build();
            return new ContextPropagatingTaskDecorator(snapshotFactory);
        }

        @Bean("emailTaskExecutor")
        ThreadPoolTaskExecutor emailTaskExecutor(TaskDecorator taskDecorator) {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setThreadNamePrefix("email-");
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setTaskDecorator(taskDecorator);
            executor.initialize();
            return executor;
        }

        @Bean
        AsyncObservedWorker asyncObservedWorker(ObservationRegistry registry) {
            return new AsyncObservedWorker(registry);
        }
    }

    static class AsyncObservedWorker {

        private final ObservationRegistry registry;

        AsyncObservedWorker(ObservationRegistry registry) {
            this.registry = registry;
        }

        @Async("emailTaskExecutor")
        @Observed(name = "test.async")
        public CompletableFuture<AsyncState> execute() {
            Observation current = registry.getCurrentObservation();
            return CompletableFuture.completedFuture(new AsyncState(
                    Thread.currentThread().getName(),
                    current.getContextView().getName(),
                    current.getContextView().getParentObservation()));
        }
    }

    private record AsyncState(
            String threadName,
            String observationName,
            ObservationView parent) {
    }
}
