package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * La barandilla contra la doble facturacion, ejercitada contra la columna
 * generada de verdad.
 *
 * <p>
 * <b>Son dos mecanismos que tienen que decir lo mismo, y nadie los habia
 * enfrentado.</b> Por un lado {@code recurring_cycle_marker}, columna
 * {@code STORED} que la base calcula con un {@code CASE} y que
 * {@code uq_sbd_recurring_cycle} convierte en un indice unico sobre
 * {@code (marker, period_start, period_end)}. Por otro
 * {@code countRecurringCycle}, una {@code @Query} nativa que <b>replica ese
 * mismo {@code CASE} a mano</b> en su {@code WHERE}.
 *
 * <p>
 * <b>Si la replica y la columna divergen, el fallo no es un error de
 * validacion: es un 500 a mitad del cierre mensual.</b> La consulta diria que
 * el periodo esta libre, el caso de uso emitiria, y el {@code INSERT} moriria
 * contra el indice unico. Y al reves —si la consulta fuera mas estricta que el
 * indice— se rechazarian cobros legitimos. Un test de servicio con el
 * repositorio mockeado no puede ver ninguna de las dos cosas: el mock devuelve
 * lo que se le diga.
 *
 * <p>
 * <b>El caso que da nombre a la trampa</b> es
 * {@link PeriodoExacto#la_anual_de_mitad_de_agosto_convive_con_la_mensual_del_dia_1}.
 * El periodo se compara por igualdad de los dos extremos y <b>no por mes</b>:
 * agrupando por mes, la factura anual emitida el 15 de agosto chocaba con la
 * mensual del dia 1 y <b>el cambio a plan anual era irregistrable</b>.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("recurring_cycle_marker — la barandilla contra la doble facturacion contra MySQL real")
class RecurringCycleMarkerPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDate MENSUAL_INICIO = LocalDate.of(2026, 8, 1);
    private static final LocalDate MENSUAL_FIN = LocalDate.of(2026, 8, 31);
    private static final LocalDate ANUAL_INICIO = LocalDate.of(2026, 8, 15);
    private static final LocalDate ANUAL_FIN = LocalDate.of(2027, 8, 14);

    @Autowired
    private JpaBillingDocumentRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Nested
    @DisplayName("Periodo exacto, no mes — TRAMPA 3")
    class PeriodoExacto {

        @Test
        @DisplayName("la factura ANUAL del 15 de agosto convive con la MENSUAL del dia 1:"
                + " agrupar por mes hacia irregistrable el cambio a plan anual")
        void la_anual_de_mitad_de_agosto_convive_con_la_mensual_del_dia_1() {
            factura(9_100L, "DC-CICLO-0001", MENSUAL_INICIO, MENSUAL_FIN);
            factura(9_101L, "DC-CICLO-0002", ANUAL_INICIO, ANUAL_FIN);

            assertThat(marcador(9_100L)).isEqualTo(SchemaSeed.SUBSCRIPTION_ID);
            assertThat(marcador(9_101L)).isEqualTo(SchemaSeed.SUBSCRIPTION_ID);
            assertThat(repository.existsRecurringCycle(SchemaSeed.COMPANY_ID,
                    SchemaSeed.SUBSCRIPTION_ID, MENSUAL_INICIO, MENSUAL_FIN)).isTrue();
            assertThat(repository.existsRecurringCycle(SchemaSeed.COMPANY_ID,
                    SchemaSeed.SUBSCRIPTION_ID, ANUAL_INICIO, ANUAL_FIN)).isTrue();
        }

        @Test
        @DisplayName("un periodo que solapa pero no coincide en los dos extremos queda libre")
        void un_periodo_que_solapa_pero_no_coincide_queda_libre() {
            factura(9_100L, "DC-CICLO-0001", MENSUAL_INICIO, MENSUAL_FIN);

            assertThat(repository.existsRecurringCycle(SchemaSeed.COMPANY_ID,
                    SchemaSeed.SUBSCRIPTION_ID, MENSUAL_INICIO, LocalDate.of(2026, 8, 30)))
                    .isFalse();
        }

        @Test
        @DisplayName("el mismo periodo exacto lo rechaza la BASE, no solo el caso de uso:"
                + " uq_sbd_recurring_cycle es la barandilla que no se puede saltar")
        void el_mismo_periodo_exacto_lo_rechaza_la_base() {
            factura(9_100L, "DC-CICLO-0001", MENSUAL_INICIO, MENSUAL_FIN);

            assertThatThrownBy(() -> factura(9_102L, "DC-CICLO-0003", MENSUAL_INICIO, MENSUAL_FIN))
                    .hasStackTraceContaining("uq_sbd_recurring_cycle");
        }
    }

    @Nested
    @DisplayName("Que entra en el marcador y que no")
    class QueEntraEnElMarcador {

        @Test
        @DisplayName("un prorrateo del MISMO periodo exacto no colisiona: su marcador es nulo"
                + " y bloquearlo rechazaria un cobro legitimo")
        void un_prorrateo_del_mismo_periodo_no_colisiona() {
            factura(9_100L, "DC-CICLO-0001", MENSUAL_INICIO, MENSUAL_FIN);
            documento(9_103L, "DC-CICLO-0004", "INVOICE", "PRORATION", "DRAFT", MENSUAL_INICIO,
                    MENSUAL_FIN);

            assertThat(marcador(9_103L)).isNull();
        }

        @Test
        @DisplayName("una nota credito del MISMO periodo tampoco colisiona: no es una factura"
                + " de ciclo")
        void una_nota_credito_del_mismo_periodo_no_colisiona() {
            factura(9_100L, "DC-CICLO-0001", MENSUAL_INICIO, MENSUAL_FIN);
            documento(9_104L, "NC-CICLO-0001", "CREDIT_NOTE", "RECURRING_CYCLE", "DRAFT",
                    MENSUAL_INICIO, MENSUAL_FIN);

            assertThat(marcador(9_104L)).isNull();
        }

        @Test
        @DisplayName("anular libera el periodo: el marcador se vuelve nulo y el ciclo se puede"
                + " reemitir, o un error de septiembre lo haria irrecuperable para siempre")
        void anular_libera_el_periodo() {
            factura(9_100L, "DC-CICLO-0001", MENSUAL_INICIO, MENSUAL_FIN);
            anular(9_100L);

            assertThat(marcador(9_100L)).isNull();
            assertThat(repository.existsRecurringCycle(SchemaSeed.COMPANY_ID,
                    SchemaSeed.SUBSCRIPTION_ID, MENSUAL_INICIO, MENSUAL_FIN)).isFalse();

            factura(9_105L, "DC-CICLO-0005", MENSUAL_INICIO, MENSUAL_FIN);

            assertThat(marcador(9_105L)).isEqualTo(SchemaSeed.SUBSCRIPTION_ID);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("el mismo periodo en otro contrato convive: el marcador es el contrato,"
                + " no la fecha")
        void el_mismo_periodo_en_otro_contrato_convive() {
            factura(9_100L, "DC-CICLO-0001", MENSUAL_INICIO, MENSUAL_FIN);
            documentoDe(9_106L, "DC-CICLO-0006", SchemaSeed.OTRA_COMPANY_ID,
                    SchemaSeed.OTRA_SUBSCRIPTION_ID, "INVOICE", "RECURRING_CYCLE", "DRAFT",
                    MENSUAL_INICIO, MENSUAL_FIN);

            assertThat(marcador(9_106L)).isEqualTo(SchemaSeed.OTRA_SUBSCRIPTION_ID);
        }

        @Test
        @DisplayName("la consulta no ve el ciclo de otra clinica")
        void la_consulta_no_ve_el_ciclo_ajeno() {
            documentoDe(9_106L, "DC-CICLO-0006", SchemaSeed.OTRA_COMPANY_ID,
                    SchemaSeed.OTRA_SUBSCRIPTION_ID, "INVOICE", "RECURRING_CYCLE", "DRAFT",
                    MENSUAL_INICIO, MENSUAL_FIN);

            assertThat(repository.existsRecurringCycle(SchemaSeed.COMPANY_ID,
                    SchemaSeed.SUBSCRIPTION_ID, MENSUAL_INICIO, MENSUAL_FIN)).isFalse();
            assertThat(repository.existsRecurringCycle(SchemaSeed.OTRA_COMPANY_ID,
                    SchemaSeed.OTRA_SUBSCRIPTION_ID, MENSUAL_INICIO, MENSUAL_FIN)).isTrue();
        }
    }

    private void factura(Long id, String numero, LocalDate inicio, LocalDate fin) {
        documento(id, numero, "INVOICE", "RECURRING_CYCLE", "DRAFT", inicio, fin);
    }

    private void documento(Long id, String numero, String kind, String reason, String status,
            LocalDate inicio, LocalDate fin) {
        documentoDe(id, numero, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, kind, reason,
                status, inicio, fin);
    }

    private void documentoDe(Long id, String numero, Long companyId, Long subscriptionId,
            String kind, String reason, String status, LocalDate inicio, LocalDate fin) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_billing_documents (id, document_number, company_id,
                                                            subscription_id, document_kind,
                                                            billing_reason, period_start,
                                                            period_end, issue_status,
                                                            subtotal_amount, tax_amount,
                                                            total_amount, settled_amount,
                                                            created_date, version)
                VALUES (:id, :numero, :companyId, :subscriptionId, :kind, :reason, :inicio,
                        :fin, :status, 100000.00, 19000.00, 119000.00, 0.00, NOW(), 0)
                """).setParameter("id", id).setParameter("numero", numero)
                .setParameter("companyId", companyId).setParameter("subscriptionId", subscriptionId)
                .setParameter("kind", kind).setParameter("reason", reason)
                .setParameter("status", status).setParameter("inicio", inicio)
                .setParameter("fin", fin).executeUpdate();
    }

    /**
     * El marcador es {@code STORED}: la base lo recalcula sola al mover
     * {@code issue_status}, sin que ninguna sentencia lo nombre.
     */
    private void anular(Long id) {
        entityManager
                .createNativeQuery(
                        "UPDATE subscription_billing_documents SET issue_status = 'VOIDED',"
                                + " version = version + 1 WHERE id = :id")
                .setParameter("id", id).executeUpdate();
    }

    private Long marcador(Long id) {
        Number valor = (Number) entityManager
                .createNativeQuery("SELECT d.recurring_cycle_marker"
                        + " FROM subscription_billing_documents d WHERE d.id = :id")
                .setParameter("id", id).getSingleResult();
        return valor == null ? null : valor.longValue();
    }
}
