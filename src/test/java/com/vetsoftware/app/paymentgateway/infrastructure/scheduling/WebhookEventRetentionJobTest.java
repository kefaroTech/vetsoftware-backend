package com.vetsoftware.app.paymentgateway.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.paymentgateway.application.port.in.PurgeExpiredWebhookEventsUseCase;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookEventRetentionJob")
class WebhookEventRetentionJobTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-04T09:05:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime CORTE = LocalDateTime
            .ofInstant(RELOJ.instant(), RELOJ.getZone()).minusDays(90);

    @Mock
    private PurgeExpiredWebhookEventsUseCase worker;

    private CapturaDeObservacion captura;

    private WebhookEventRetentionJob job() {
        return new WebhookEventRetentionJob(worker, new SystemAuthRunner(), captura.telemetria(),
                RELOJ, 90);
    }

    @BeforeEach
    void montar() {
        captura = new CapturaDeObservacion();
    }

    @Test
    @DisplayName("rechaza un plazo de retencion no positivo")
    void rechaza_retencion_no_positiva() {
        assertThatThrownBy(() -> new WebhookEventRetentionJob(worker, new SystemAuthRunner(),
                captura.telemetria(), RELOJ, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("sin filas que purgar: no_work")
    void sin_filas_que_purgar_no_work() {
        when(worker.purgeRawBodyOlderThan(CORTE)).thenReturn(0);

        job().purgeExpiredRawBodies();

        assertThat(captura.resultado()).isEqualTo("no_work");
    }

    @Test
    @DisplayName("purga filas: success, con el corte calculado desde el reloj y los dias configurados")
    void purga_filas_success() {
        when(worker.purgeRawBodyOlderThan(CORTE)).thenReturn(5);

        job().purgeExpiredRawBodies();

        verify(worker).purgeRawBodyOlderThan(eq(CORTE));
        assertThat(captura.resultado()).isEqualTo("success");
        assertThat(captura.nombreDelJob()).isEqualTo("webhook.event.retention");
    }

    @Nested
    @DisplayName("candado distribuido")
    class CandadoDistribuido {

        @Test
        @DisplayName("con el candado en manos de otra instancia, no purga")
        void con_lock_ajeno_no_purga() throws Throwable {
            LockProvider lockProvider = lockConfig -> Optional.empty();
            LockingTaskExecutor executor = new DefaultLockingTaskExecutor(lockProvider);
            LockConfiguration lockConfiguration = new LockConfiguration(Instant.now(),
                    "webhook.event.retention", Duration.ofMinutes(10), Duration.ofMinutes(1));

            executor.executeWithLock((LockingTaskExecutor.Task) () -> job().purgeExpiredRawBodies(),
                    lockConfiguration);

            verifyNoInteractions(worker);
        }
    }

    private static final class CapturaDeObservacion
            implements
                ObservationHandler<Observation.Context> {

        private final ObservationRegistry registry = ObservationRegistry.create();
        private final List<String> etiquetas = new ArrayList<>();

        private CapturaDeObservacion() {
            registry.observationConfig().observationHandler(this);
        }

        ScheduledJobTelemetry telemetria() {
            return new ScheduledJobTelemetry(registry);
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }

        @Override
        public void onStop(Observation.Context context) {
            context.getLowCardinalityKeyValues()
                    .forEach(kv -> etiquetas.add(kv.getKey() + "=" + kv.getValue()));
        }

        @Override
        public void onError(Observation.Context context) {
            onStop(context);
        }

        String resultado() {
            return valorDe("job.outcome");
        }

        String nombreDelJob() {
            return valorDe("job.name");
        }

        private String valorDe(String clave) {
            return etiquetas.stream().filter(e -> e.startsWith(clave + "="))
                    .map(e -> e.substring(clave.length() + 1)).findFirst().orElse(null);
        }
    }
}
