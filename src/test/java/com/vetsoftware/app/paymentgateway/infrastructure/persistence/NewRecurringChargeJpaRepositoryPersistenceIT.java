package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code NewRecurringChargeJpaRepository} contra MySQL real.
 *
 * <p>
 * La consulta nativa es un unico {@code SELECT} con dos {@code NOT EXISTS} y un
 * filtro sobre el estado de la suscripcion: cada rama solo la ve un test contra
 * el motor, nunca un doble.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("NewRecurringChargeJpaRepository — primer cobro de un ciclo recurrente")
class NewRecurringChargeJpaRepositoryPersistenceIT extends AbstractDataJpaTest {

    private static final BigDecimal TOTAL = new BigDecimal("119000.00");

    @Autowired
    private NewRecurringChargeJpaRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Nested
    @DisplayName("Candidato limpio")
    class CandidatoLimpio {

        @Test
        @DisplayName("un documento RECURRING_CYCLE con saldo y sin intentos entra en la cola")
        void un_documento_con_saldo_entra_en_la_cola() {
            documento(9000L, "FV-NRC-0001", "2026-03-01", "2026-03-31");
            entityManager.flush();

            assertThat(repository.findNewRecurringChargesAfter(0L, 100))
                    .extracting(SubscriptionBillingDocumentJpaEntity::getId).containsExactly(9000L);
        }
    }

    @Nested
    @DisplayName("Estado de la suscripcion")
    class EstadoDeLaSuscripcion {

        @Test
        @DisplayName("una suscripcion CANCELLED no ofrece sus documentos")
        void una_suscripcion_cancelled_no_ofrece_documentos() {
            cambiarEstadoSuscripcion("CANCELLED");
            documento(9001L, "FV-NRC-0002", "2026-03-01", "2026-03-31");
            entityManager.flush();

            assertThat(repository.findNewRecurringChargesAfter(0L, 100)).isEmpty();
        }

        @Test
        @DisplayName("una suscripcion EXPIRED tampoco")
        void una_suscripcion_expired_tampoco() {
            cambiarEstadoSuscripcion("EXPIRED");
            documento(9002L, "FV-NRC-0003", "2026-03-01", "2026-03-31");
            entityManager.flush();

            assertThat(repository.findNewRecurringChargesAfter(0L, 100)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Pago ya aplicado")
    class PagoYaAplicado {

        @Test
        @DisplayName("un pago PENDING aplicado al documento lo saca de la cola")
        void un_pago_pending_aplicado_lo_saca_de_la_cola() {
            documento(9003L, "FV-NRC-0004", "2026-03-01", "2026-03-31");
            pago(9500L, "PENDING");
            aplicacionDePago(9600L, 9003L, 9500L);
            entityManager.flush();

            assertThat(repository.findNewRecurringChargesAfter(0L, 100)).isEmpty();
        }

        @Test
        @DisplayName("un pago REFUNDED aplicado al documento tampoco lo reabre")
        void un_pago_refunded_aplicado_no_lo_reabre() {
            documento(9004L, "FV-NRC-0005", "2026-03-01", "2026-03-31");
            pago(9501L, "REFUNDED");
            aplicacionDePago(9601L, 9004L, 9501L);
            entityManager.flush();

            assertThat(repository.findNewRecurringChargesAfter(0L, 100)).isEmpty();
        }

        @Test
        @DisplayName("un pago CONFIRMED no esta en la exclusion: el documento sigue en la cola")
        void un_pago_confirmed_no_excluye_por_si_mismo() {
            documento(9005L, "FV-NRC-0006", "2026-03-01", "2026-03-31");
            pago(9502L, "CONFIRMED");
            aplicacionDePago(9602L, 9005L, 9502L);
            entityManager.flush();

            assertThat(repository.findNewRecurringChargesAfter(0L, 100))
                    .extracting(SubscriptionBillingDocumentJpaEntity::getId).containsExactly(9005L);
        }
    }

    @Nested
    @DisplayName("Intento ya hecho")
    class IntentoYaHecho {

        @Test
        @DisplayName("un documento con un intento de cobro ya no es 'nuevo'")
        void un_documento_con_intento_ya_no_es_nuevo() {
            documento(9006L, "FV-NRC-0007", "2026-03-01", "2026-03-31");
            intento(9700L, 9006L);
            entityManager.flush();

            assertThat(repository.findNewRecurringChargesAfter(0L, 100)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Forma y filtros de la consulta")
    class FormaYFiltrosDeLaConsulta {

        @Test
        @DisplayName("VOIDED no entra aunque tenga saldo")
        void voided_no_entra() {
            documento(9007L, "FV-NRC-0008", "2026-03-01", "2026-03-31");
            anular(9007L);
            entityManager.flush();

            assertThat(repository.findNewRecurringChargesAfter(0L, 100)).isEmpty();
        }

        @Test
        @DisplayName("sin saldo (settled = total) no entra")
        void sin_saldo_no_entra() {
            documento(9008L, "FV-NRC-0009", "2026-03-01", "2026-03-31");
            saldar(9008L);
            entityManager.flush();

            assertThat(repository.findNewRecurringChargesAfter(0L, 100)).isEmpty();
        }

        @Test
        @DisplayName("una razon distinta de RECURRING_CYCLE no entra")
        void otra_razon_no_entra() {
            documentoConRazon(9009L, "FV-NRC-0010", "ONE_TIME", "2026-03-01", "2026-03-31");
            entityManager.flush();

            assertThat(repository.findNewRecurringChargesAfter(0L, 100)).isEmpty();
        }

        @Test
        @DisplayName("el cursor por id deja fuera lo ya visto")
        void el_cursor_deja_fuera_lo_ya_visto() {
            documento(9010L, "FV-NRC-0011", "2026-03-01", "2026-03-31");
            documento(9011L, "FV-NRC-0012", "2026-04-01", "2026-04-30");
            entityManager.flush();

            assertThat(repository.findNewRecurringChargesAfter(9010L, 100))
                    .extracting(SubscriptionBillingDocumentJpaEntity::getId).containsExactly(9011L);
        }

        @Test
        @DisplayName("el tamano de lote acota cuantos trae, en orden de id")
        void el_tamano_de_lote_acota() {
            documento(9012L, "FV-NRC-0013", "2026-03-01", "2026-03-31");
            documento(9013L, "FV-NRC-0014", "2026-04-01", "2026-04-30");
            entityManager.flush();

            assertThat(repository.findNewRecurringChargesAfter(0L, 1))
                    .extracting(SubscriptionBillingDocumentJpaEntity::getId).containsExactly(9012L);
        }
    }

    // --- andamio ------------------------------------------------------------

    private void documento(Long id, String numero, String inicio, String fin) {
        documentoConRazon(id, numero, "RECURRING_CYCLE", inicio, fin);
    }

    private void documentoConRazon(Long id, String numero, String razon, String inicio,
            String fin) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_billing_documents (id, document_number, company_id,
                                                            subscription_id, document_kind,
                                                            billing_reason, period_start,
                                                            period_end, issue_status,
                                                            subtotal_amount, tax_amount,
                                                            total_amount, settled_amount,
                                                            created_date, version)
                VALUES (:id, :numero, :companyId, :subscriptionId, 'INVOICE', :razon,
                        :inicio, :fin, 'DRAFT', 100000.00, 19000.00, :total, 0.00, NOW(), 0)
                """).setParameter("id", id).setParameter("numero", numero)
                .setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("subscriptionId", SchemaSeed.SUBSCRIPTION_ID)
                .setParameter("razon", razon).setParameter("inicio", inicio)
                .setParameter("fin", fin).setParameter("total", TOTAL).executeUpdate();
    }

    private void anular(Long id) {
        entityManager.createNativeQuery(
                "update subscription_billing_documents set issue_status = 'VOIDED' where id = :id")
                .setParameter("id", id).executeUpdate();
    }

    private void saldar(Long id) {
        entityManager
                .createNativeQuery("update subscription_billing_documents "
                        + "set settled_amount = total_amount where id = :id")
                .setParameter("id", id).executeUpdate();
    }

    private void cambiarEstadoSuscripcion(String estado) {
        entityManager.createNativeQuery("update subscriptions set status = :estado where id = :id")
                .setParameter("estado", estado).setParameter("id", SchemaSeed.SUBSCRIPTION_ID)
                .executeUpdate();
    }

    private void pago(Long id, String estado) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_payments (id, company_id, amount, currency,
                                                   payment_method, gateway, gateway_reference,
                                                   received_at, status, reconciled_at,
                                                   client_request_id, created_date, version)
                VALUES (:id, :companyId, 119000.00, 'COP', 'TRANSFER', NULL, NULL,
                        '2026-03-05 09:00:00', :estado, NULL, NULL, NOW(), 0)
                """).setParameter("id", id).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("estado", estado).executeUpdate();
    }

    private void aplicacionDePago(Long id, Long documentoId, Long pagoId) {
        entityManager.createNativeQuery("""
                INSERT INTO billing_document_applications (id, company_id, target_document_id,
                                                           source_kind, payment_id,
                                                           source_document_id, applied_amount,
                                                           reversal_of_id, applied_at, value_date,
                                                           client_request_id, created_date)
                VALUES (:id, :companyId, :documentoId, 'PAYMENT', :pagoId, NULL, 119000.00,
                        NULL, '2026-03-05 09:30:00', '2026-03-05', NULL, NOW())
                """).setParameter("id", id).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("documentoId", documentoId).setParameter("pagoId", pagoId)
                .executeUpdate();
    }

    private void intento(Long id, Long documentoId) {
        entityManager.createNativeQuery("""
                INSERT INTO payment_attempts (id, company_id, billing_document_id,
                                              payment_method_id, attempt_number, gateway,
                                              requested_amount, gateway_decline_code,
                                              decline_kind, attempted_at, next_attempt_at,
                                              created_date, version)
                VALUES (:id, :companyId, :documentoId, NULL, 1, 'wompi', 119000.00,
                        NULL, 'CONFIGURATION', NOW(), NULL, NOW(), 0)
                """).setParameter("id", id).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("documentoId", documentoId).executeUpdate();
    }
}
