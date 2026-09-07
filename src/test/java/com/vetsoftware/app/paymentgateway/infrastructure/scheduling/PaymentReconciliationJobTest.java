package com.vetsoftware.app.paymentgateway.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.paymentgateway.application.dto.ReconciliationBatchResult;
import com.vetsoftware.app.paymentgateway.application.port.in.ReconcilePendingPaymentsUseCase;
import com.vetsoftware.app.paymentgateway.infrastructure.gateway.WompiProperties;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentReconciliationJob")
class PaymentReconciliationJobTest {

    private static final int TAMANO_LOTE = 50;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-04T10:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime CORTE = LocalDateTime
            .ofInstant(RELOJ.instant(), RELOJ.getZone()).minusMinutes(60);

    @Mock
    private ReconcilePendingPaymentsUseCase worker;

    private CapturaDeObservacion captura;

    private PaymentReconciliationJob job(boolean wompiEnabled) {
        WompiProperties properties = new WompiProperties(wompiEnabled,
                "https://sandbox.wompi.co/v1", "pub_test_x", "prv_test_x", "secret", "secret", 6,
                Duration.ofSeconds(2), Duration.ofHours(24), 65536L, Duration.ofHours(24));
        return new PaymentReconciliationJob(worker, new SystemAuthRunner(), captura.telemetria(),
                properties, RELOJ, 60L, TAMANO_LOTE);
    }

    @BeforeEach
    void montar() {
        captura = new CapturaDeObservacion();
    }

    @Test
    @DisplayName("WOMPI_ENABLED=false: no toca ningun puerto y se reporta no_work")
    void wompi_deshabilitado_no_toca_ningun_puerto() {
        job(false).runReconciliation();

        verifyNoInteractions(worker);
        assertThat(captura.resultado()).isEqualTo("no_work");
    }

    @Test
    @DisplayName("consulta con el corte calculado desde el reloj inyectado")
    void consulta_con_el_corte_calculado() {
        when(worker.reconcileOlderThan(eq(CORTE), eq(TAMANO_LOTE)))
                .thenReturn(new ReconciliationBatchResult(0, 0, 0));

        job(true).runReconciliation();

        verify(worker).reconcileOlderThan(CORTE, TAMANO_LOTE);
        assertThat(captura.resultado()).isEqualTo("no_work");
    }

    @Test
    @DisplayName("candidatos resueltos sin fallos: success")
    void candidatos_resueltos_sin_fallos() {
        when(worker.reconcileOlderThan(any(), eq(TAMANO_LOTE)))
                .thenReturn(new ReconciliationBatchResult(3, 2, 0));

        job(true).runReconciliation();

        assertThat(captura.resultado()).isEqualTo("success");
    }

    @Test
    @DisplayName("un lote completo pide el siguiente hasta que sale corto (RES2-45)")
    void un_lote_completo_pide_el_siguiente() {
        when(worker.reconcileOlderThan(any(), eq(TAMANO_LOTE)))
                .thenReturn(new ReconciliationBatchResult(TAMANO_LOTE, TAMANO_LOTE, 0))
                .thenReturn(new ReconciliationBatchResult(10, 10, 0));

        job(true).runReconciliation();

        verify(worker, times(2)).reconcileOlderThan(any(), eq(TAMANO_LOTE));
        assertThat(captura.resultado()).isEqualTo("success");
    }

    @Test
    @DisplayName("lotes completos sin fin se detienen en el tope de iteraciones (RES2-45)")
    void lotes_completos_sin_fin_se_detienen_en_el_tope() {
        when(worker.reconcileOlderThan(any(), eq(TAMANO_LOTE)))
                .thenReturn(new ReconciliationBatchResult(TAMANO_LOTE, 0, 0));

        job(true).runReconciliation();

        verify(worker, times(PaymentReconciliationJob.MAX_ITERATIONS)).reconcileOlderThan(any(),
                eq(TAMANO_LOTE));
    }

    @Test
    @DisplayName("todos los candidatos fallan contra la pasarela: failure")
    void todos_fallan() {
        when(worker.reconcileOlderThan(any(), eq(TAMANO_LOTE)))
                .thenReturn(new ReconciliationBatchResult(2, 0, 2));

        job(true).runReconciliation();

        assertThat(captura.resultado()).isEqualTo("failure");
        assertThat(captura.nombreDelJob()).isEqualTo("payment.reconciliation");
    }

    @Nested
    @DisplayName("candado distribuido (#772/#773)")
    class CandadoDistribuido {

        @Test
        @DisplayName("con el candado en manos de otra instancia, no concilia")
        void con_lock_ajeno_no_concilia() throws Throwable {
            LockProvider lockProvider = lockConfig -> Optional.empty();
            LockingTaskExecutor executor = new DefaultLockingTaskExecutor(lockProvider);
            LockConfiguration lockConfiguration = new LockConfiguration(Instant.now(),
                    "payment.reconciliation", Duration.ofMinutes(50), Duration.ofMinutes(1));

            executor.executeWithLock((LockingTaskExecutor.Task) () -> job(true).runReconciliation(),
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
