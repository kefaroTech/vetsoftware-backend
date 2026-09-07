package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.paymentgateway.domain.GatewayWebhookOutcome;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaGatewayWebhookEventRecorderPort} /
 * {@code WompiWebhookEventJpaRepository} contra MySQL real.
 *
 * <p>
 * Lo que solo se puede comprobar aqui es la unicidad
 * {@code uq_gateway_webhook_events_checksum} sobre el par
 * {@code (gateway, event_checksum)}: es la autoridad de idempotencia del
 * webhook, y ningun doble de
 * {@link com.vetsoftware.app.paymentgateway.application.port.out.GatewayWebhookEventRecorderPort}
 * la ejercita. Junto a ella van los dos {@code CHECK} del changeset 411 que
 * atan {@code processed_at} y {@code processing_outcome}.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaGatewayWebhookEventRecorderPort — eventos de webhook contra MySQL real")
class WompiWebhookEventPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDateTime RECIBIDO = LocalDateTime.of(2026, 3, 12, 9, 30, 15);
    private static final LocalDateTime PROCESADO = LocalDateTime.of(2026, 3, 12, 9, 30, 20);
    private static final LocalDateTime CORTE_RETENCION = LocalDateTime.of(2026, 6, 1, 0, 0);

    @Autowired
    private JpaGatewayWebhookEventRecorderPort recorder;
    @Autowired
    private WompiWebhookEventJpaRepository jpaRepository;
    @PersistenceContext
    private EntityManager entityManager;

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("recordReceived guarda el evento sin procesar, campo a campo")
        void guarda_el_evento_sin_procesar() {
            Long id = recorder.recordReceived("WOMPI", "transaction.updated", "chk-alta-0001",
                    "TX-REF-0001", RECIBIDO, "{\"event\":\"transaction.updated\"}");
            entityManager.flush();
            entityManager.clear();

            assertThat(jpaRepository.findById(id)).get().satisfies(evento -> {
                assertThat(evento.getGateway()).isEqualTo("WOMPI");
                assertThat(evento.getEventType()).isEqualTo("transaction.updated");
                assertThat(evento.getEventChecksum()).isEqualTo("chk-alta-0001");
                assertThat(evento.getGatewayReference()).isEqualTo("TX-REF-0001");
                assertThat(evento.getReceivedAt()).isEqualTo(RECIBIDO);
                assertThat(evento.getRawBody()).isEqualTo("{\"event\":\"transaction.updated\"}");
                assertThat(evento.getCreatedDate()).isEqualTo(RECIBIDO);
                assertThat(evento.getProcessedAt()).isNull();
                assertThat(evento.getProcessingOutcome()).isNull();
                assertThat(evento.getVersion()).isZero();
            });
        }

        @Test
        @DisplayName("recordOutcome rellena el desenlace y sube la version por el ciclo leer-modificar-guardar")
        void recordOutcome_rellena_el_desenlace_y_sube_la_version() {
            Long id = recorder.recordReceived("WOMPI", "transaction.updated", "chk-outcome-0001",
                    "TX-REF-0002", RECIBIDO, "{}");
            entityManager.flush();
            entityManager.clear();

            recorder.recordOutcome(id, PROCESADO, GatewayWebhookOutcome.APPLIED);
            entityManager.flush();
            entityManager.clear();

            assertThat(jpaRepository.findById(id)).get().satisfies(evento -> {
                assertThat(evento.getProcessedAt()).isEqualTo(PROCESADO);
                assertThat(evento.getProcessingOutcome())
                        .isEqualTo(GatewayWebhookOutcome.APPLIED.name());
                assertThat(evento.getVersion()).isEqualTo(1L);
            });
        }

        @Test
        @DisplayName("recordOutcome sobre un id inexistente no inventa una fila")
        void recordOutcome_sobre_id_inexistente_no_inventa_una_fila() {
            assertThatThrownBy(() -> recorder.recordOutcome(999_999L, PROCESADO,
                    GatewayWebhookOutcome.APPLIED)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("999999");
        }
    }

    @Nested
    @DisplayName("existsByChecksum")
    class Idempotencia {

        @Test
        @DisplayName("ve el evento ya recibido de la misma pasarela")
        void ve_el_evento_ya_recibido() {
            recorder.recordReceived("WOMPI", "transaction.updated", "chk-exists-0001", null,
                    RECIBIDO, "{}");
            entityManager.flush();

            assertThat(recorder.existsByChecksum("WOMPI", "chk-exists-0001")).isTrue();
        }

        @Test
        @DisplayName("un checksum distinto no existe")
        void un_checksum_distinto_no_existe() {
            recorder.recordReceived("WOMPI", "transaction.updated", "chk-exists-0002", null,
                    RECIBIDO, "{}");
            entityManager.flush();

            assertThat(recorder.existsByChecksum("WOMPI", "chk-otro")).isFalse();
        }

        @Test
        @DisplayName("el mismo checksum en OTRA pasarela no cuenta: la unicidad es del par")
        void el_mismo_checksum_en_otra_pasarela_no_cuenta() {
            recorder.recordReceived("WOMPI", "transaction.updated", "chk-cruzado", null, RECIBIDO,
                    "{}");
            entityManager.flush();

            assertThat(recorder.existsByChecksum("PAYU", "chk-cruzado")).isFalse();
        }
    }

    @Nested
    @DisplayName("Unicidad (gateway, event_checksum)")
    class UnicidadDelChecksum {

        @Test
        @DisplayName("reintentar el mismo evento de la misma pasarela lo para uq_gateway_webhook_events_checksum")
        void el_mismo_evento_repetido_lo_para_la_unicidad() {
            recorder.recordReceived("WOMPI", "transaction.updated", "chk-duplicado", null, RECIBIDO,
                    "{}");
            entityManager.flush();

            EngineConstraint.assertViolates("uq_gateway_webhook_events_checksum", () -> {
                recorder.recordReceived("WOMPI", "transaction.updated", "chk-duplicado", null,
                        RECIBIDO, "{}");
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("el mismo checksum en OTRA pasarela si entra: la unicidad es del par")
        void el_mismo_checksum_en_otra_pasarela_si_entra() {
            recorder.recordReceived("WOMPI", "transaction.updated", "chk-par-0001", null, RECIBIDO,
                    "{}");
            recorder.recordReceived("PAYU", "transaction.updated", "chk-par-0001", null, RECIBIDO,
                    "{}");
            entityManager.flush();
            entityManager.clear();

            assertThat(recorder.existsByChecksum("WOMPI", "chk-par-0001")).isTrue();
            assertThat(recorder.existsByChecksum("PAYU", "chk-par-0001")).isTrue();
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("un processed_at sin desenlace lo para chk_gwe_processed")
        void processed_at_sin_desenlace_lo_para_el_check() {
            EngineConstraint.assertViolates("chk_gwe_processed",
                    () -> insertarCrudo(9_100L, "chk-check-1", PROCESADO, null));
        }

        @Test
        @DisplayName("un desenlace fuera del vocabulario lo para chk_gwe_processing_outcome")
        void un_desenlace_no_admitido_lo_para_el_check() {
            EngineConstraint.assertViolates("chk_gwe_processing_outcome",
                    () -> insertarCrudo(9_101L, "chk-check-2", PROCESADO, "DUPLICATE"));
        }
    }

    @Nested
    @DisplayName("purgeRawBodyOlderThan (#791)")
    class RetencionDeCuerpoCrudo {

        @Test
        @DisplayName("vacia raw_body de lo recibido antes del corte y sube la version")
        void vacia_raw_body_de_lo_anterior_al_corte() {
            Long viejo = recorder.recordReceived("WOMPI", "transaction.updated", "chk-purga-vieja",
                    null, CORTE_RETENCION.minusDays(1), "{\"card\":\"visa\"}");
            entityManager.flush();
            entityManager.clear();

            int purgadas = recorder.purgeRawBodyOlderThan(CORTE_RETENCION);
            entityManager.clear();

            assertThat(purgadas).isEqualTo(1);
            assertThat(jpaRepository.findById(viejo)).get().satisfies(evento -> {
                assertThat(evento.getRawBody()).isNull();
                assertThat(evento.getVersion()).isEqualTo(1L);
            });
        }

        @Test
        @DisplayName("no toca lo recibido en o despues del corte")
        void no_toca_lo_recibido_despues_del_corte() {
            Long reciente = recorder.recordReceived("WOMPI", "transaction.updated",
                    "chk-purga-reciente", null, CORTE_RETENCION.plusDays(1), "{\"card\":\"visa\"}");
            entityManager.flush();
            entityManager.clear();

            int purgadas = recorder.purgeRawBodyOlderThan(CORTE_RETENCION);
            entityManager.clear();

            assertThat(purgadas).isZero();
            assertThat(jpaRepository.findById(reciente)).get()
                    .satisfies(evento -> assertThat(evento.getRawBody()).isNotNull());
        }

        @Test
        @DisplayName("una fila ya sin cuerpo crudo no cuenta de nuevo")
        void una_fila_ya_purgada_no_cuenta_de_nuevo() {
            Long viejo = recorder.recordReceived("WOMPI", "transaction.updated", "chk-purga-doble",
                    null, CORTE_RETENCION.minusDays(1), "{}");
            entityManager.flush();
            entityManager.clear();
            recorder.purgeRawBodyOlderThan(CORTE_RETENCION);
            entityManager.clear();

            int segundaPasada = recorder.purgeRawBodyOlderThan(CORTE_RETENCION);

            assertThat(segundaPasada).isZero();
        }
    }

    private void insertarCrudo(Long id, String checksum, LocalDateTime processedAt,
            String processingOutcome) {
        entityManager.createNativeQuery("""
                INSERT INTO gateway_webhook_events (id, gateway, event_type, event_checksum,
                        received_at, raw_body, processed_at, processing_outcome, created_date,
                        version)
                VALUES (:id, 'WOMPI', 'transaction.updated', :checksum, :recibido, '{}',
                        :processedAt, :outcome, :recibido, 0)
                """).setParameter("id", id).setParameter("checksum", checksum)
                .setParameter("recibido", RECIBIDO).setParameter("processedAt", processedAt)
                .setParameter("outcome", processingOutcome).executeUpdate();
    }
}
