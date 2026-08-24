package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * La aritmetica del saldo, contra MySQL real y por primera vez.
 *
 * <p>
 * <b>Esta consulta no se puede probar con mocks y no habia nada que la
 * ejecutara.</b> {@code JpaBillingDocumentSettlementPort} no encaja en el
 * naming {@code Jpa<Algo>Repository} de {@code ADAPTADOR_JPA_CON_RODAJA}, asi
 * que ninguna regla exigia su rodaja; y los tres casos de uso que la invocan
 * ({@code ApplyBillingDocumentService},
 * {@code ChangeSubscriptionPaymentStatusService},
 * {@code ReverseBillingDocumentApplicationService}) la tienen mockeada, que es
 * lo correcto en un test de servicio y lo que deja el SQL entero sin red.
 *
 * <p>
 * <b>El caso que justifica la clase entera es
 * {@link SumaLasAplicaciones#la_nota_credito_con_payment_id_nulo_entra_en_la_suma}.</b>
 * El {@code LEFT JOIN} contra {@code subscription_payments} es lo unico que
 * mantiene dentro de la suma a las aplicaciones de nota credito, que llevan
 * {@code payment_id} nulo. Con un {@code JOIN} normal esas filas desaparecen,
 * {@code settled_amount} se queda corto y el saldo no baja nunca aunque al
 * cliente se le haya acreditado el dinero: una clinica en solo lectura por una
 * deuda que ya no existe. Cambiar {@code LEFT JOIN} por {@code JOIN} es una
 * palabra, se lee bien en un diff y hasta hoy no lo detectaba nada.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("BillingDocumentSettlementJpaRepository — el saldo se recalcula de cero contra MySQL real")
class BillingDocumentSettlementPersistenceIT extends AbstractDataJpaTest {

    private static final Long FACTURA_ID = 8_200L;
    private static final Long NOTA_CREDITO_ID = 8_201L;
    private static final Long FACTURA_AJENA_ID = 8_202L;
    private static final Long PAGO_CONFIRMADO_ID = 8_210L;
    private static final Long PAGO_PENDIENTE_ID = 8_211L;
    private static final Long APLICACION_DEL_PAGO_ID = 8_300L;

    private static final BigDecimal TOTAL_FACTURA = new BigDecimal("200000.00");

    @Autowired
    private JpaBillingDocumentSettlementPort settlementPort;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        documento(FACTURA_ID, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, "DCT-SET-0001",
                "INVOICE", TOTAL_FACTURA);
        documento(NOTA_CREDITO_ID, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                "NCT-SET-0001", "CREDIT_NOTE", new BigDecimal("50000.00"));
        documento(FACTURA_AJENA_ID, SchemaSeed.OTRA_COMPANY_ID, SchemaSeed.OTRA_SUBSCRIPTION_ID,
                "DCT-SET-0002", "INVOICE", TOTAL_FACTURA);
        pago(PAGO_CONFIRMADO_ID, SchemaSeed.COMPANY_ID, new BigDecimal("120000.00"), "CONFIRMED");
        pago(PAGO_PENDIENTE_ID, SchemaSeed.COMPANY_ID, new BigDecimal("30000.00"), "PENDING");
    }

    @Nested
    @DisplayName("Suma las aplicaciones que cuentan como cobro")
    class SumaLasAplicaciones {

        @Test
        @DisplayName("un pago CONFIRMED aplicado reduce el saldo por su importe")
        void un_pago_confirmado_reduce_el_saldo() {
            aplicacionDePago(APLICACION_DEL_PAGO_ID, PAGO_CONFIRMADO_ID,
                    new BigDecimal("120000.00"));

            int filas = settlementPort.recalculateSettledAmount(FACTURA_ID, SchemaSeed.COMPANY_ID);

            assertThat(filas).isOne();
            assertThat(saldado(FACTURA_ID)).isEqualByComparingTo("120000.00");
            assertThat(saldo(FACTURA_ID)).isEqualByComparingTo("80000.00");
        }

        @Test
        @DisplayName("la aplicacion de nota credito, con payment_id NULO, entra en la suma:"
                + " con un JOIN normal desapareceria y el saldo no bajaria jamas")
        void la_nota_credito_con_payment_id_nulo_entra_en_la_suma() {
            aplicacionDeNotaCredito(8_301L, NOTA_CREDITO_ID, new BigDecimal("50000.00"));

            settlementPort.recalculateSettledAmount(FACTURA_ID, SchemaSeed.COMPANY_ID);

            assertThat(saldado(FACTURA_ID)).isEqualByComparingTo("50000.00");
            assertThat(saldo(FACTURA_ID)).isEqualByComparingTo("150000.00");
        }

        @Test
        @DisplayName("pago y nota credito conviven en la misma suma: 120.000 + 50.000")
        void pago_y_nota_credito_conviven_en_la_misma_suma() {
            aplicacionDePago(APLICACION_DEL_PAGO_ID, PAGO_CONFIRMADO_ID,
                    new BigDecimal("120000.00"));
            aplicacionDeNotaCredito(8_301L, NOTA_CREDITO_ID, new BigDecimal("50000.00"));

            settlementPort.recalculateSettledAmount(FACTURA_ID, SchemaSeed.COMPANY_ID);

            assertThat(saldado(FACTURA_ID)).isEqualByComparingTo("170000.00");
            assertThat(saldo(FACTURA_ID)).isEqualByComparingTo("30000.00");
        }

        @Test
        @DisplayName("un pago PENDING aplicado no reduce el saldo: la pasarela aviso pero"
                + " no confirmo")
        void un_pago_pendiente_no_reduce_el_saldo() {
            aplicacionDePago(8_302L, PAGO_PENDIENTE_ID, new BigDecimal("30000.00"));

            settlementPort.recalculateSettledAmount(FACTURA_ID, SchemaSeed.COMPANY_ID);

            assertThat(saldado(FACTURA_ID)).isEqualByComparingTo("0.00");
            assertThat(saldo(FACTURA_ID)).isEqualByComparingTo(TOTAL_FACTURA);
        }

        @Test
        @DisplayName("una contra-aplicacion negativa devuelve el importe: la suma es neta,"
                + " no un acumulado de lo que entro")
        void una_contra_aplicacion_devuelve_el_importe() {
            aplicacionDePago(APLICACION_DEL_PAGO_ID, PAGO_CONFIRMADO_ID,
                    new BigDecimal("120000.00"));
            aplicacionDeNotaCredito(8_301L, NOTA_CREDITO_ID, new BigDecimal("50000.00"));
            reversaDePago(8_303L, PAGO_CONFIRMADO_ID, APLICACION_DEL_PAGO_ID,
                    new BigDecimal("-120000.00"));

            settlementPort.recalculateSettledAmount(FACTURA_ID, SchemaSeed.COMPANY_ID);

            assertThat(saldado(FACTURA_ID)).isEqualByComparingTo("50000.00");
            assertThat(saldo(FACTURA_ID)).isEqualByComparingTo("150000.00");
        }

        @Test
        @DisplayName("sin ninguna aplicacion el saldado vuelve a cero, no se queda con el"
                + " ultimo valor: recalcula de cero, no acumula")
        void sin_aplicaciones_vuelve_a_cero() {
            aplicacionDePago(APLICACION_DEL_PAGO_ID, PAGO_CONFIRMADO_ID,
                    new BigDecimal("120000.00"));
            settlementPort.recalculateSettledAmount(FACTURA_ID, SchemaSeed.COMPANY_ID);
            entityManager
                    .createNativeQuery("DELETE FROM billing_document_applications WHERE id = :id")
                    .setParameter("id", APLICACION_DEL_PAGO_ID).executeUpdate();

            settlementPort.recalculateSettledAmount(FACTURA_ID, SchemaSeed.COMPANY_ID);

            assertThat(saldado(FACTURA_ID)).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la factura de otra clinica no se toca y devuelve cero filas:"
                + " es lo que el llamador interpreta como no existe")
        void la_factura_ajena_no_se_toca() {
            int filas = settlementPort.recalculateSettledAmount(FACTURA_AJENA_ID,
                    SchemaSeed.COMPANY_ID);

            assertThat(filas).isZero();
            assertThat(saldado(FACTURA_AJENA_ID)).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("una factura propia pedida con la empresa equivocada tampoco se mueve")
        void la_factura_propia_con_empresa_equivocada_no_se_mueve() {
            aplicacionDePago(APLICACION_DEL_PAGO_ID, PAGO_CONFIRMADO_ID,
                    new BigDecimal("120000.00"));

            int filas = settlementPort.recalculateSettledAmount(FACTURA_ID,
                    SchemaSeed.OTRA_COMPANY_ID);

            assertThat(filas).isZero();
            assertThat(saldado(FACTURA_ID)).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("un documento que no existe devuelve cero filas")
        void un_documento_inexistente_devuelve_cero() {
            assertThat(settlementPort.recalculateSettledAmount(8_999L, SchemaSeed.COMPANY_ID))
                    .isZero();
        }
    }

    @Nested
    @DisplayName("Bloqueo optimista — incidencia #53")
    class BloqueoOptimista {

        @Test
        @DisplayName("el UPDATE masivo mueve la version: sin eso un save concurrente que"
                + " venga de una lectura anterior pisa el recalculo sin ruido")
        void el_update_masivo_mueve_la_version() {
            long antes = version(FACTURA_ID);
            aplicacionDePago(APLICACION_DEL_PAGO_ID, PAGO_CONFIRMADO_ID,
                    new BigDecimal("120000.00"));

            settlementPort.recalculateSettledAmount(FACTURA_ID, SchemaSeed.COMPANY_ID);

            assertThat(version(FACTURA_ID)).isEqualTo(antes + 1);
        }

        @Test
        @DisplayName("un recalculo que no cambia el importe mueve la version igual:"
                + " la fila se escribio y quien la tenia leida ya no esta al dia")
        void un_recalculo_sin_cambio_mueve_la_version_igual() {
            long antes = version(FACTURA_ID);

            settlementPort.recalculateSettledAmount(FACTURA_ID, SchemaSeed.COMPANY_ID);
            settlementPort.recalculateSettledAmount(FACTURA_ID, SchemaSeed.COMPANY_ID);

            assertThat(version(FACTURA_ID)).isEqualTo(antes + 2);
        }
    }

    @Nested
    @DisplayName("balance_amount es de la base, no del codigo")
    class SaldoCalculado {

        @Test
        @DisplayName("la entidad no expone ningun mutador del saldo: su ausencia es la"
                + " barrera que impide desincronizarlo")
        void la_entidad_no_expone_mutador_del_saldo() {
            assertThat(SubscriptionBillingDocumentJpaEntity.class.getMethods())
                    .extracting(Method::getName).doesNotContain("setBalanceAmount");
        }

        @Test
        @DisplayName("el saldo lo recalcula la base sola tras el UPDATE, sin que ninguna"
                + " sentencia lo nombre")
        void el_saldo_lo_recalcula_la_base_sola() {
            aplicacionDePago(APLICACION_DEL_PAGO_ID, PAGO_CONFIRMADO_ID,
                    new BigDecimal("120000.00"));
            aplicacionDeNotaCredito(8_301L, NOTA_CREDITO_ID, new BigDecimal("50000.00"));

            settlementPort.recalculateSettledAmount(FACTURA_ID, SchemaSeed.COMPANY_ID);

            assertThat(saldo(FACTURA_ID))
                    .isEqualByComparingTo(TOTAL_FACTURA.subtract(saldado(FACTURA_ID)));
        }
    }

    private void documento(Long id, Long companyId, Long subscriptionId, String numero, String kind,
            BigDecimal total) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_billing_documents (id, document_number, company_id,
                                                            subscription_id, document_kind,
                                                            billing_reason, period_start,
                                                            period_end, issue_status,
                                                            subtotal_amount, tax_amount,
                                                            total_amount, settled_amount,
                                                            created_date, version)
                VALUES (:id, :numero, :companyId, :subscriptionId, :kind, 'ONE_TIME',
                        '2026-02-01', '2026-02-28', 'DRAFT', :total, 0.00, :total, 0.00,
                        NOW(), 0)
                """).setParameter("id", id).setParameter("numero", numero)
                .setParameter("companyId", companyId).setParameter("subscriptionId", subscriptionId)
                .setParameter("kind", kind).setParameter("total", total).executeUpdate();
    }

    private void pago(Long id, Long companyId, BigDecimal importe, String estado) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_payments (id, company_id, amount, currency,
                                                   payment_method, gateway, gateway_reference,
                                                   received_at, status, reconciled_at,
                                                   client_request_id, created_date, version)
                VALUES (:id, :companyId, :importe, 'COP', 'TRANSFER', NULL, NULL,
                        '2026-02-05 09:00:00', :estado, NULL, NULL, NOW(), 0)
                """).setParameter("id", id).setParameter("companyId", companyId)
                .setParameter("importe", importe).setParameter("estado", estado).executeUpdate();
    }

    private void aplicacionDePago(Long id, Long pagoId, BigDecimal importe) {
        aplicacion(id, "PAYMENT", pagoId, null, importe, null);
    }

    private void aplicacionDeNotaCredito(Long id, Long notaCreditoId, BigDecimal importe) {
        aplicacion(id, "CREDIT_NOTE", null, notaCreditoId, importe, null);
    }

    private void reversaDePago(Long id, Long pagoId, Long reversaDe, BigDecimal importe) {
        aplicacion(id, "PAYMENT", pagoId, null, importe, reversaDe);
    }

    private void aplicacion(Long id, String sourceKind, Long pagoId, Long documentoOrigenId,
            BigDecimal importe, Long reversaDe) {
        entityManager.createNativeQuery("""
                INSERT INTO billing_document_applications (id, company_id, target_document_id,
                                                           source_kind, payment_id,
                                                           source_document_id, applied_amount,
                                                           reversal_of_id, applied_at,
                                                           client_request_id, created_date)
                VALUES (:id, :companyId, :target, :sourceKind, :pagoId, :origen, :importe,
                        :reversaDe, '2026-02-05 09:30:00', NULL, NOW())
                """).setParameter("id", id).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("target", FACTURA_ID).setParameter("sourceKind", sourceKind)
                .setParameter("pagoId", pagoId).setParameter("origen", documentoOrigenId)
                .setParameter("importe", importe).setParameter("reversaDe", reversaDe)
                .executeUpdate();
    }

    private BigDecimal saldado(Long documentoId) {
        return (BigDecimal) escalar("settled_amount", documentoId);
    }

    private BigDecimal saldo(Long documentoId) {
        return (BigDecimal) escalar("balance_amount", documentoId);
    }

    private long version(Long documentoId) {
        return ((Number) escalar("version", documentoId)).longValue();
    }

    private Object escalar(String columna, Long documentoId) {
        return entityManager
                .createNativeQuery("SELECT d." + columna
                        + " FROM subscription_billing_documents d WHERE d.id = :id")
                .setParameter("id", documentoId).getSingleResult();
    }
}
