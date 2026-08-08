package com.vetsoftware.app.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.context.integration.Slf4jThreadLocalAccessor;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

class AsyncConfigTest {

    private AsyncTaskExecutor executor;

    @AfterEach
    void cleanUp() {
        MDC.clear();
        if (executor != null) {
            ((SimpleAsyncTaskExecutor) executor).close();
        }
    }

    @Test
    void propagatesObservationAndSelectedMdcWithoutLeakingContext() throws Exception {
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        ContextRegistry contextRegistry = new ContextRegistry()
                .registerThreadLocalAccessor(
                        new ObservationThreadLocalAccessor(observationRegistry))
                .registerThreadLocalAccessor(new Slf4jThreadLocalAccessor(MdcKeys.ACTOR_TYPE,
                        MdcKeys.ACTOR_EMPLOYEE_ID, MdcKeys.ACTOR_COMPANY_ID,
                        MdcKeys.ACTOR_SYSTEM_USER_ID, MdcKeys.HTTP_METHOD, MdcKeys.HTTP_PATH));
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
                .contextRegistry(contextRegistry).build();
        ContextPropagatingTaskDecorator decorator = new ContextPropagatingTaskDecorator(
                snapshotFactory);

        executor = new AsyncConfig().emailTaskExecutor(decorator);

        Observation parent = Observation.start("test.request", observationRegistry);
        AsyncContext propagated;
        try (Observation.Scope ignored = parent.openScope()) {
            MDC.put(MdcKeys.ACTOR_TYPE, "EMPLOYEE");
            MDC.put(MdcKeys.ACTOR_EMPLOYEE_ID, "42");
            MDC.put(MdcKeys.HTTP_METHOD, "POST");
            MDC.put(MdcKeys.HTTP_PATH, "/emails");
            MDC.put(MdcKeys.CLIENT_IP, "192.0.2.10");

            propagated = executor.submit(() -> captureContext(observationRegistry)).get(5,
                    TimeUnit.SECONDS);
        } finally {
            MDC.clear();
            parent.stop();
        }

        assertThat(propagated.threadName()).startsWith("email-");
        assertThat(propagated.observation()).isSameAs(parent);
        assertThat(propagated.actorType()).isEqualTo("EMPLOYEE");
        assertThat(propagated.actorEmployeeId()).isEqualTo("42");
        assertThat(propagated.httpMethod()).isEqualTo("POST");
        assertThat(propagated.httpPath()).isEqualTo("/emails");
        assertThat(propagated.clientIp()).isNull();

        // Cada tarea corre en un hilo virtual nuevo, pero el decorador debe limpiar
        // igual: ninguna tarea puede heredar el contexto de la anterior.
        for (int i = 0; i < 4; i++) {
            AsyncContext clean = executor.submit(() -> captureContext(observationRegistry)).get(5,
                    TimeUnit.SECONDS);
            assertThat(clean.observation()).isNull();
            assertThat(clean.actorType()).isNull();
            assertThat(clean.actorEmployeeId()).isNull();
            assertThat(clean.httpMethod()).isNull();
            assertThat(clean.httpPath()).isNull();
            assertThat(clean.clientIp()).isNull();
        }
    }

    private static AsyncContext captureContext(ObservationRegistry observationRegistry) {
        return new AsyncContext(Thread.currentThread().getName(),
                observationRegistry.getCurrentObservation(), MDC.get(MdcKeys.ACTOR_TYPE),
                MDC.get(MdcKeys.ACTOR_EMPLOYEE_ID), MDC.get(MdcKeys.HTTP_METHOD),
                MDC.get(MdcKeys.HTTP_PATH), MDC.get(MdcKeys.CLIENT_IP));
    }

    private record AsyncContext(String threadName, Observation observation, String actorType,
            String actorEmployeeId, String httpMethod, String httpPath, String clientIp) {
    }
}
