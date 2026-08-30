package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscriptionpayment.domain.WithholdingRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
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
 * La lectura de la retención con la que se salda una factura, contra MySQL
 * real.
 *
 * <p>
 * <b>Por qué esta rodaja hacía falta.</b> El adaptador se llama {@code ...Port}
 * y no {@code Jpa<Algo>Repository}, así que {@code ADAPTADOR_JPA_CON_RODAJA} no
 * lo alcanza y su SQL solo lo ejercitaban mocks de Mockito en los casos de uso.
 * En un puerto donde <b>{@code AND w.company_id = :companyId} es la garantía de
 * aislamiento entera</b>, un mock no prueba absolutamente nada: devuelve lo que
 * el test le dijo, con cláusula o sin ella.
 *
 * <p>
 * <b>Lo que decide.</b> Cuánto se da por pagado de una factura con una cifra
 * que se declara ante la DIAN. Sin la acotación por empresa, una clínica podría
 * saldar su factura con la retención practicada a otra: la cartera cuadraría en
 * las dos y la declaración de ninguna.
 *
 * <p>
 * <b>Cómo está montado para que un SQL equivocado se vea.</b> Hay dos
 * retenciones de la misma clínica sobre documentos distintos, así que perder el
 * {@code w.id} devuelve la otra en vez de vacío —el {@code setMaxResults(1)} lo
 * taparía—; hay una retención de otra clínica, así que perder el
 * {@code company_id} se caza en vez de acertar por coincidencia; y los cuatro
 * escalares de la fila son distintos entre sí, así que cruzar dos columnas
 * rompe el caso de mapeo.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaWithholdingQueryPort — la retención acotada por empresa contra MySQL real")
class WithholdingQueryPortIT extends AbstractDataJpaTest {

    private static final Long FACTURA_PROPIA = 7310L;
    private static final Long OTRA_FACTURA_PROPIA = 7311L;
    private static final Long FACTURA_AJENA = 7312L;

    private static final Long RETENCION_PROPIA = 7320L;
    /**
     * Segunda retención de la misma clínica: sin ella, perder el {@code id} no se
     * vería.
     */
    private static final Long OTRA_RETENCION_PROPIA = 7321L;
    /** La retención de la otra clínica. La mitad del valor de esta rodaja. */
    private static final Long RETENCION_AJENA = 7322L;

    /**
     * Los tres importes, deliberadamente distintos y ninguno igual a una base ni a
     * un identificador: si {@code amount} y {@code taxable_base} cambiaran de sitio
     * en el {@code SELECT}, con importes parecidos no se notaría.
     */
    private static final BigDecimal IMPORTE_PROPIO = new BigDecimal("125000.00");
    private static final BigDecimal IMPORTE_PROPIO_DOS = new BigDecimal("250000.00");
    private static final BigDecimal IMPORTE_AJENO = new BigDecimal("375000.00");
    private static final BigDecimal BASE = new BigDecimal("5000000.00");

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JpaWithholdingQueryPort port;

    @BeforeEach
    void sembrarLasRetenciones() {
        SchemaSeed.seed(entityManager);

        factura(FACTURA_PROPIA, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, "FAC-7310",
                "2026-04-01", "2026-04-30");
        factura(OTRA_FACTURA_PROPIA, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, "FAC-7311",
                "2026-05-01", "2026-05-31");
        factura(FACTURA_AJENA, SchemaSeed.OTRA_COMPANY_ID, SchemaSeed.OTRA_SUBSCRIPTION_ID,
                "FAC-7312", "2026-04-01", "2026-04-30");

        // Retenciones de renta: `chk_document_withholdings_municipality` exige
        // municipio NULO fuera de ICA, y `chk_..._period` exige la clave de periodo
        // '<ano>-A'. Se eligen de renta justamente para no tocar `cities.dane_code`,
        // que es unico global y ya lo ocupa otra rodaja.
        retencion(RETENCION_PROPIA, SchemaSeed.COMPANY_ID, FACTURA_PROPIA, IMPORTE_PROPIO);
        retencion(OTRA_RETENCION_PROPIA, SchemaSeed.COMPANY_ID, OTRA_FACTURA_PROPIA,
                IMPORTE_PROPIO_DOS);
        retencion(RETENCION_AJENA, SchemaSeed.OTRA_COMPANY_ID, FACTURA_AJENA, IMPORTE_AJENO);

        entityManager.flush();
    }

    @Nested
    @DisplayName("Mapeo posicional")
    class MapeoPosicional {

        @Test
        @DisplayName("cada escalar del SELECT cae en el campo que dice: id, empresa, factura e importe")
        void cada_escalar_cae_en_el_campo_que_dice() {
            assertThat(port.findByIdAndCompanyId(RETENCION_PROPIA, SchemaSeed.COMPANY_ID)).get()
                    .usingRecursiveComparison()
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(new WithholdingRef(RETENCION_PROPIA, SchemaSeed.COMPANY_ID,
                            FACTURA_PROPIA, IMPORTE_PROPIO));
        }

        /**
         * El {@code billingDocumentId} es lo que impide saldar la factura de septiembre
         * con la retención de la de agosto. Si esa columna trajera otra cosa, la
         * comprobación del caso de uso pasaría a ser decorativa.
         */
        @Test
        @DisplayName("la retención sabe de qué factura es, y no de la otra")
        void la_retencion_sabe_de_que_factura_es() {
            assertThat(port.findByIdAndCompanyId(RETENCION_PROPIA, SchemaSeed.COMPANY_ID)).get()
                    .satisfies(retencion -> {
                        assertThat(retencion.esDelDocumento(FACTURA_PROPIA)).isTrue();
                        assertThat(retencion.esDelDocumento(OTRA_FACTURA_PROPIA)).isFalse();
                    });
        }

        @Test
        @DisplayName("devuelve la retención pedida y no otra de la misma clínica")
        void devuelve_la_retencion_pedida_y_no_otra_de_la_misma_clinica() {
            // Las dos filas existen y son de la misma empresa: si el `w.id` se perdiera,
            // el `setMaxResults(1)` devolveria una de las dos en silencio en vez de fallar.
            assertThat(retencionesDe(SchemaSeed.COMPANY_ID)).isEqualTo(2);

            assertThat(port.findByIdAndCompanyId(OTRA_RETENCION_PROPIA, SchemaSeed.COMPANY_ID))
                    .get().extracting(WithholdingRef::id).isEqualTo(OTRA_RETENCION_PROPIA);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * <b>El caso que justifica esta rodaja.</b> Quitar
         * {@code AND w.company_id = :companyId} deja el {@code SELECT} devolviendo la
         * retención de la otra clínica, y el caso de uso la aplicaría contra una
         * factura que no es suya.
         */
        @Test
        @DisplayName("la retención de otra clínica no se lee, aunque el id exista")
        void la_retencion_de_otra_clinica_no_se_lee() {
            // Se comprueba que la fila EXISTE antes de exigir que no se vea: sin esto el
            // caso pasaria por ausencia de dato y no por la clausula de empresa, que es la
            // forma silenciosa de que una prueba de aislamiento deje de proteger.
            assertThat(retencionesConId(RETENCION_AJENA)).isEqualTo(1);

            assertThat(port.findByIdAndCompanyId(RETENCION_AJENA, SchemaSeed.COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("y tampoco en el sentido contrario")
        void y_tampoco_en_el_sentido_contrario() {
            assertThat(port.findByIdAndCompanyId(RETENCION_PROPIA, SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
        }

        @Test
        @DisplayName("cada clínica lee la suya")
        void cada_clinica_lee_la_suya() {
            assertThat(port.findByIdAndCompanyId(RETENCION_AJENA, SchemaSeed.OTRA_COMPANY_ID)).get()
                    .extracting(WithholdingRef::companyId).isEqualTo(SchemaSeed.OTRA_COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("Ausencias")
    class Ausencias {

        @Test
        @DisplayName("una retención que no existe no se inventa")
        void una_retencion_que_no_existe_no_se_inventa() {
            assertThat(port.findByIdAndCompanyId(7399L, SchemaSeed.COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("sin id o sin empresa no se consulta la base")
        void sin_id_o_sin_empresa_no_se_consulta_la_base() {
            assertThat(port.findByIdAndCompanyId(null, SchemaSeed.COMPANY_ID)).isEmpty();
            assertThat(port.findByIdAndCompanyId(RETENCION_PROPIA, null)).isEmpty();
        }
    }

    private long retencionesConId(Long id) {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM document_withholdings WHERE id = :id")
                .setParameter("id", id).getSingleResult()).longValue();
    }

    private long retencionesDe(Long companyId) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM document_withholdings WHERE company_id = :companyId
                """).setParameter("companyId", companyId).getSingleResult()).longValue();
    }

    private void retencion(Long id, Long companyId, Long billingDocumentId, BigDecimal importe) {
        entityManager.createNativeQuery("""
                INSERT INTO document_withholdings (id, company_id, billing_document_id,
                                                   withholding_type, taxable_base, rate_percent,
                                                   amount, municipality_code, fiscal_year,
                                                   fiscal_period_key, practiced_on, certificate_id,
                                                   created_date, version)
                VALUES (:id, :companyId, :facturaId, 'INCOME_TAX', :base, 2.500000, :importe,
                        NULL, 2026, '2026-A', '2026-04-15', NULL, '2026-04-16 09:30:00', 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("companyId", companyId)
                .setParameter("facturaId", billingDocumentId).setParameter("base", BASE)
                .setParameter("importe", importe).executeUpdate();
    }

    private void factura(Long id, Long companyId, Long subscriptionId, String numero,
            String periodoInicio, String periodoFin) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_billing_documents (id, document_number, company_id,
                                                            subscription_id, document_kind,
                                                            billing_reason, period_start,
                                                            period_end, issue_status,
                                                            subtotal_amount, tax_amount,
                                                            total_amount, settled_amount,
                                                            created_date, version)
                VALUES (:id, :numero, :companyId, :contrato, 'INVOICE', 'RECURRING_CYCLE',
                        :periodoInicio, :periodoFin, 'DRAFT', 5000000.00, 0.00, 5000000.00,
                        0.00, '2026-04-01 00:00:00', 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("numero", numero)
                .setParameter("companyId", companyId).setParameter("contrato", subscriptionId)
                .setParameter("periodoInicio", java.time.LocalDate.parse(periodoInicio))
                .setParameter("periodoFin", java.time.LocalDate.parse(periodoFin)).executeUpdate();
    }
}
