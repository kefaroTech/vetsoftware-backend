package com.vetsoftware.app.revenuerecognitionline.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.revenuerecognitionline.domain.RecognitionMethod;
import com.vetsoftware.app.revenuerecognitionline.domain.RevenueRecognitionLine;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaRevenueRecognitionLineRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para demostrar es que la correccion de un
 * reconocimiento SE PUEDE ESCRIBIR</b>, que es exactamente lo que la clave del
 * documento maestro impedia. Con {@code (charge_id, period_key)} a secas, la
 * fila que compensa —mismo cargo, mismo mes, importe opuesto— chocaba contra la
 * unicidad y el libro no dejaba escribir su propia correccion.
 * {@code uq_rrl_recognition} lleva {@code posting_period} dentro justo para
 * eso: {@link Unicidad#la_fila_que_compensa_cabe_en_otro_periodo_contable()} y
 * {@link Unicidad#el_reintento_en_el_mismo_periodo_choca()} son las dos mitades
 * de esa afirmacion.
 *
 * <p>
 * <b>Y comprueba la unica de las cuatro reglas de periodo que la base impone
 * sola</b>: {@code chk_rrl_not_backwards}. Con formato {@code AAAA-MM} y
 * colacion {@code ascii_bin}, la comparacion lexicografica <em>es</em> la
 * cronologica, y eso es lo que hace que el informe de marzo siga dando lo que
 * se declaro.
 *
 * <p>
 * <b>El seed no trae ni periodos contables ni cargos.</b> Los dos periodos y el
 * cargo se insertan por SQL nativo con ids del rango <b>8420</b>, que ninguna
 * otra rodaja usa, y con claves de mes de <b>2028</b> para no chocar con las
 * que usan la rodaja de conciliacion externa (2026) ni la de periodos (2027).
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaRevenueRecognitionLineRepository — el libro de ingreso contra MySQL real")
class RevenueRecognitionLinePersistenceIT extends AbstractDataJpaTest {

    private static final Long PERIODO_MARZO_ID = 8420L;
    private static final Long PERIODO_ABRIL_ID = 8421L;
    private static final Long CARGO_ID = 8422L;
    private static final Long RENGLON_CRUDO = 8423L;

    private static final String MARZO = "2028-03";
    private static final String ABRIL = "2028-04";

    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2028, 4, 1, 2, 0, 0);

    @Autowired
    private RevenueRecognitionLineJpaRepository springDataRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaRevenueRecognitionLineRepository repository;

    @BeforeEach
    void adaptador() {
        SchemaSeed.seed(entityManager);
        periodo(PERIODO_MARZO_ID, MARZO);
        periodo(PERIODO_ABRIL_ID, ABRIL);
        cargo();
        entityManager.flush();
        repository = new JpaRevenueRecognitionLineRepository(springDataRepository,
                new RevenueRecognitionLineJpaMapper());
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda el renglon y lo recupera con cada campo en su sitio")
        void guarda_el_renglon_y_lo_recupera_campo_a_campo() {
            RevenueRecognitionLine guardado = repository
                    .save(renglon(MARZO, MARZO, new BigDecimal("125000.00")));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(recuperado -> {
                assertThat(recuperado.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                assertThat(recuperado.getChargeId()).isEqualTo(CARGO_ID);
                assertThat(recuperado.getPeriodKey()).isEqualTo(MARZO);
                assertThat(recuperado.getPostingPeriod()).isEqualTo(MARZO);
                assertThat(recuperado.getRecognizedAmount()).isEqualByComparingTo("125000.00");
                assertThat(recuperado.getMethod()).isEqualTo(RecognitionMethod.STRAIGHT_LINE_DAYS);
                assertThat(recuperado.getCreatedDate()).isEqualTo(CREADO_EL);
                assertThat(recuperado.isOffset()).isFalse();
            });
        }

        @Test
        @DisplayName("la variante acotada por empresa no devuelve el renglon de otra clinica")
        void la_variante_acotada_no_devuelve_el_renglon_ajeno() {
            RevenueRecognitionLine guardado = repository
                    .save(renglon(MARZO, MARZO, new BigDecimal("125000.00")));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .isPresent();
            assertThat(
                    repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
        }

        @Test
        @DisplayName("el barrido por periodo contable devuelve el renglon de la clinica")
        void el_barrido_por_periodo_devuelve_el_renglon() {
            repository.save(renglon(MARZO, MARZO, new BigDecimal("125000.00")));
            entityManager.flush();
            entityManager.clear();

            PageResult<RevenueRecognitionLine> pagina = repository.findAllByPostingPeriod(MARZO, 0,
                    20);

            assertThat(pagina.content()).hasSize(1);
            assertThat(pagina.totalElements()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Unicidad y correccion")
    class Unicidad {

        @Test
        @DisplayName("la fila que compensa cabe si se registra en otro periodo contable")
        void la_fila_que_compensa_cabe_en_otro_periodo_contable() {
            // EL caso de la feature. Con la clave del documento maestro esta segunda
            // fila era inescribible y el libro no podia corregirse a si mismo.
            RevenueRecognitionLine original = repository
                    .save(renglon(MARZO, MARZO, new BigDecimal("125000.00")));
            entityManager.flush();
            entityManager.clear();

            RevenueRecognitionLine compensa = repository.save(
                    repository.findById(original.getId()).orElseThrow().offsetIn(ABRIL, CREADO_EL));
            entityManager.flush();

            assertThat(compensa.getId()).isNotNull();
            assertThat(compensa.getRecognizedAmount()).isEqualByComparingTo("-125000.00");
            assertThat(compensa.isOffset()).isTrue();
        }

        @Test
        @DisplayName("el reintento en el mismo periodo choca: es la llave antiduplicados")
        void el_reintento_en_el_mismo_periodo_choca() {
            repository.save(renglon(MARZO, MARZO, new BigDecimal("125000.00")));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_rrl_recognition", () -> {
                repository.save(renglon(MARZO, MARZO, new BigDecimal("125000.00")));
                entityManager.flush();
            });
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("imputar hacia atras lo para chk_rrl_not_backwards")
        void imputar_hacia_atras_lo_para_el_check() {
            // El dominio ya lo rechaza; esto comprueba que el motor tambien. Sin esta
            // barandilla, un hecho tardio podria reescribir el ingreso de un mes ya
            // declarado y el informe de marzo dejaria de dar lo que se declaro.
            EngineConstraint.assertViolates("chk_rrl_not_backwards",
                    () -> insertarCrudo(RENGLON_CRUDO, ABRIL, MARZO, "125000.00",
                            "STRAIGHT_LINE_DAYS"));
        }

        @Test
        @DisplayName("un importe cero lo para chk_rrl_amount")
        void un_importe_cero_lo_para_el_check_del_importe() {
            // Una fila de importe cero no compensa nada y solo ensucia el libro.
            EngineConstraint.assertViolates("chk_rrl_amount", () -> insertarCrudo(RENGLON_CRUDO + 1,
                    MARZO, MARZO, "0.00", "STRAIGHT_LINE_DAYS"));
        }

        @Test
        @DisplayName("un metodo desconocido lo para chk_rrl_method")
        void un_metodo_desconocido_lo_para_el_check_del_metodo() {
            EngineConstraint.assertViolates("chk_rrl_method",
                    () -> insertarCrudo(RENGLON_CRUDO + 2, MARZO, MARZO, "125000.00", "MILESTONE"));
        }
    }

    private static RevenueRecognitionLine renglon(String periodKey, String postingPeriod,
            BigDecimal importe) {
        return RevenueRecognitionLine.record(SchemaSeed.COMPANY_ID, CARGO_ID, periodKey,
                postingPeriod, importe, RecognitionMethod.STRAIGHT_LINE_DAYS, CREADO_EL);
    }

    private void periodo(Long id, String clave) {
        entityManager.createNativeQuery("""
                INSERT INTO accounting_periods (id, period_key, status, created_date, version)
                VALUES (:id, :clave, 'OPEN', NOW(6), 0)
                """).setParameter("id", id).setParameter("clave", clave).executeUpdate();
    }

    /**
     * El cargo del que cuelga el reconocimiento. Solo las columnas obligatorias:
     * este andamio existe para satisfacer la clave foranea compuesta
     * {@code fk_rrl_charge (company_id, charge_id)}, no para montar un escenario de
     * facturacion.
     *
     * <p>
     * <strong>{@code ONE_TIME} y no {@code RECURRING}, y no es
     * indiferente:</strong> {@code chk_subscription_charges_recurring_item} exige
     * que un cargo recurrente nombre la linea de contrato de la que sale, y este
     * andamio no monta ninguna. Al libro de reconocimiento le da igual la clase de
     * cargo —lo unico que necesita es que la pareja (empresa, cargo) exista— asi
     * que se elige la que no arrastra media capa de suscripciones detras.
     */
    private void cargo() {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_charges (id, company_id, subscription_id, charge_type,
                        description, service_period_start, service_period_end, quantity,
                        unit_amount, subtotal_amount, tax_rate, tax_treatment, status,
                        created_date)
                VALUES (:id, :empresa, :contrato, 'ONE_TIME', 'Cargo de andamio', '2028-03-01',
                        '2028-03-31', 1.000, 125000.00, 125000.00, 0.00, 'EXCLUDED', 'PENDING',
                        NOW())
                """).setParameter("id", CARGO_ID).setParameter("empresa", SchemaSeed.COMPANY_ID)
                .setParameter("contrato", SchemaSeed.SUBSCRIPTION_ID).executeUpdate();
    }

    private void insertarCrudo(Long id, String periodKey, String postingPeriod, String importe,
            String metodo) {
        entityManager.createNativeQuery("""
                INSERT INTO revenue_recognition_lines (id, company_id, charge_id, period_key,
                        posting_period, recognized_amount, method, created_date)
                VALUES (:id, :empresa, :cargo, :periodo, :registro, :importe, :metodo, NOW(6))
                """).setParameter("id", id).setParameter("empresa", SchemaSeed.COMPANY_ID)
                .setParameter("cargo", CARGO_ID).setParameter("periodo", periodKey)
                .setParameter("registro", postingPeriod)
                .setParameter("importe", new BigDecimal(importe)).setParameter("metodo", metodo)
                .executeUpdate();
        entityManager.flush();
    }
}
