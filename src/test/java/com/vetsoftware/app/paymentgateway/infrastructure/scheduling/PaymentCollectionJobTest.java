package com.vetsoftware.app.paymentgateway.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.paymentgateway.application.dto.PaymentCollectionBatchResult;
import com.vetsoftware.app.paymentgateway.application.port.in.RunPaymentCollectionUseCase;
import com.vetsoftware.app.paymentgateway.domain.DocumentChargeOutcome;
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
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCollectionJob")
class PaymentCollectionJobTest {

    private static final int TAMANO_LOTE = 3;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-04T10:10:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.ofInstant(RELOJ.instant(),
            RELOJ.getZone());

    private static final PaymentCollectionBatchResult LOTE_VACIO = new PaymentCollectionBatchResult(
            0, 0, 0L, Map.of());

    @Mock
    private RunPaymentCollectionUseCase worker;
    @Captor
    private ArgumentCaptor<Long> cursorCaptor;
    @Captor
    private ArgumentCaptor<Integer> pageCaptor;

    private CapturaDeObservacion captura;

    private PaymentCollectionJob job(boolean wompiEnabled) {
        WompiProperties properties = new WompiProperties(wompiEnabled,
                "https://sandbox.wompi.co/v1", "pub_test_x", "prv_test_x", "secret", "secret", 6,
                Duration.ofSeconds(2));
        return new PaymentCollectionJob(worker, new SystemAuthRunner(), captura.telemetria(),
                properties, RELOJ, TAMANO_LOTE);
    }

    @BeforeEach
    void montar() {
        captura = new CapturaDeObservacion();
    }

    @Nested
    @DisplayName("WOMPI_ENABLED=false")
    class Deshabilitado {

        @Test
        @DisplayName("no toca ningun puerto y se reporta no_work")
        void no_toca_ningun_puerto() {
            job(false).runCollection();

            verifyNoInteractions(worker);
            assertThat(captura.resultado()).isEqualTo("no_work");
        }
    }

    @Nested
    @DisplayName("dos fuentes de trabajo, en orden")
    class DosFuentes {

        @Test
        @DisplayName("primero agota los documentos nuevos por cursor, despues pagina los reintentos")
        void primero_documentos_nuevos_despues_reintentos() {
            when(worker.collectNewChargesAfter(0L, TAMANO_LOTE))
                    .thenReturn(new PaymentCollectionBatchResult(TAMANO_LOTE, 0, 13L,
                            Map.of(DocumentChargeOutcome.APPROVED, TAMANO_LOTE)));
            when(worker.collectNewChargesAfter(13L, TAMANO_LOTE)).thenReturn(LOTE_VACIO);
            when(worker.collectDueRetries(AHORA, 0, TAMANO_LOTE))
                    .thenReturn(new PaymentCollectionBatchResult(1, 0, 0L,
                            Map.of(DocumentChargeOutcome.DECLINED, 1)));

            job(true).runCollection();

            verify(worker, times(2)).collectNewChargesAfter(cursorCaptor.capture(),
                    eq(TAMANO_LOTE));
            assertThat(cursorCaptor.getAllValues()).containsExactly(0L, 13L);
            verify(worker, times(1)).collectDueRetries(eq(AHORA), pageCaptor.capture(),
                    eq(TAMANO_LOTE));
            assertThat(pageCaptor.getAllValues()).containsExactly(0);
            assertThat(captura.resultado()).isEqualTo("success");
        }

        @Test
        @DisplayName("un lote lleno de reintentos vencidos encadena otra pagina")
        void un_lote_lleno_de_reintentos_encadena_otra_pagina() {
            when(worker.collectNewChargesAfter(0L, TAMANO_LOTE)).thenReturn(LOTE_VACIO);
            when(worker.collectDueRetries(AHORA, 0, TAMANO_LOTE))
                    .thenReturn(new PaymentCollectionBatchResult(TAMANO_LOTE, 0, 0L, Map.of()));
            when(worker.collectDueRetries(AHORA, 1, TAMANO_LOTE))
                    .thenReturn(new PaymentCollectionBatchResult(1, 0, 0L, Map.of()));

            job(true).runCollection();

            verify(worker, times(2)).collectDueRetries(eq(AHORA), pageCaptor.capture(),
                    eq(TAMANO_LOTE));
            assertThat(pageCaptor.getAllValues()).containsExactly(0, 1);
        }

        @Test
        @DisplayName("un cursor que no avanza corta la vuelta de documentos nuevos")
        void un_cursor_que_no_avanza_corta_la_vuelta() {
            when(worker.collectNewChargesAfter(0L, TAMANO_LOTE))
                    .thenReturn(new PaymentCollectionBatchResult(1, 0, 0L, Map.of()));
            when(worker.collectDueRetries(eq(AHORA), anyInt(), anyInt())).thenReturn(LOTE_VACIO);

            job(true).runCollection();

            verify(worker, times(1)).collectNewChargesAfter(anyLong(), anyInt());
        }
    }

    @Nested
    @DisplayName("Telemetria")
    class Telemetria {

        @Test
        @DisplayName("sin candidatos en ninguna fuente: no_work")
        void sin_candidatos_no_work() {
            when(worker.collectNewChargesAfter(0L, TAMANO_LOTE)).thenReturn(LOTE_VACIO);
            when(worker.collectDueRetries(AHORA, 0, TAMANO_LOTE)).thenReturn(LOTE_VACIO);

            job(true).runCollection();

            assertThat(captura.resultado()).isEqualTo("no_work");
            assertThat(captura.nombreDelJob()).isEqualTo("payment.collection");
        }

        @Test
        @DisplayName("todos los intentos fallan: failure")
        void todos_fallan() {
            when(worker.collectNewChargesAfter(0L, TAMANO_LOTE))
                    .thenReturn(new PaymentCollectionBatchResult(2, 2, 0L, Map.of()));
            when(worker.collectDueRetries(AHORA, 0, TAMANO_LOTE)).thenReturn(LOTE_VACIO);

            job(true).runCollection();

            assertThat(captura.resultado()).isEqualTo("failure");
        }

        @Test
        @DisplayName("algunos fallan y otros no: partial_failure")
        void algunos_fallan() {
            when(worker.collectNewChargesAfter(0L, TAMANO_LOTE))
                    .thenReturn(new PaymentCollectionBatchResult(2, 1, 0L, Map.of()));
            when(worker.collectDueRetries(AHORA, 0, TAMANO_LOTE)).thenReturn(LOTE_VACIO);

            job(true).runCollection();

            assertThat(captura.resultado()).isEqualTo("partial_failure");
        }
    }

    @Nested
    @DisplayName("Configuracion")
    class Configuracion {

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("un tamano de lote no positivo revienta al construir el bean")
        void un_tamano_de_lote_no_positivo_revienta_al_construir(int batchSize) {
            WompiProperties properties = new WompiProperties(true, "https://sandbox.wompi.co/v1",
                    "pub_test_x", "prv_test_x", "secret", "secret", 6, Duration.ofSeconds(2));

            org.assertj.core.api.Assertions
                    .assertThatThrownBy(
                            () -> new PaymentCollectionJob(worker, new SystemAuthRunner(),
                                    captura.telemetria(), properties, RELOJ, batchSize))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("batchSize");

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
