package com.vetsoftware.app.customercredit.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.customercredit.domain.CustomerCreditBalance;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaCustomerCreditBalanceRepository} contra MySQL real.
 *
 * <p>
 * <b>La barandilla del saldo a favor es una sola instruccion</b>, y esta clase
 * existe para vigilarla:
 *
 * <pre>
 * UPDATE customer_credit_balances
 *    SET balance_amount = balance_amount + :delta, version = version + 1, ...
 *  WHERE company_id = :companyId AND balance_amount + :delta &gt;= 0
 * </pre>
 *
 * <p>
 * La condicion del {@code WHERE} y el numero de filas afectadas <b>son</b> el
 * control: cero filas significa «no hay saldo» y el caso de uso aborta antes de
 * escribir un solo asiento. Un test con dobles no ve nada de esto —el doble
 * devuelve el entero que le digan—, asi que la unica red posible es el motor de
 * verdad. Aqui se comprueban el camino que mueve la fila, el que devuelve cero
 * sin tocarla, el {@code CHECK} que hay debajo por si alguien escribiera el
 * {@code UPDATE} sin su condicion, y que la version se mueve —que es lo que
 * exige {@code UPDATE_MASIVO_MUEVE_LA_VERSION} (#53)—.
 *
 * <p>
 * La carrera de dos aplicaciones simultaneas vive aparte, en
 * {@code CustomerCreditBalanceConcurrencyIT}: necesita transacciones
 * confirmadas y esta rodaja corre con rollback.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCustomerCreditBalanceRepository — el saldo a favor contra MySQL real")
class CustomerCreditBalancePersistenceIT extends AbstractDataJpaTest {

    private static final LocalDateTime ABIERTO_EL = LocalDateTime.of(2026, 1, 10, 8, 0, 0);
    private static final LocalDateTime MOVIDO_EL = LocalDateTime.of(2026, 3, 15, 17, 42, 9);

    @Autowired
    private JpaCustomerCreditBalanceRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Nested
    @DisplayName("Apertura")
    class Apertura {

        @Test
        @DisplayName("abre el saldo en cero, sin fecha de caducidad y con la marca de recalculo")
        void abre_el_saldo_en_cero() {
            repository.openIfAbsent(SchemaSeed.COMPANY_ID, ABIERTO_EL);

            assertThat(repository.findByCompanyId(SchemaSeed.COMPANY_ID)).get().satisfies(saldo -> {
                assertThat(saldo.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                assertThat(saldo.getBalanceAmount()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(saldo.getNextExpiryOn()).isNull();
                assertThat(saldo.getRecalculatedAt()).isEqualTo(ABIERTO_EL);
                assertThat(saldo.hasCredit()).isFalse();
            });
        }

        @Test
        @DisplayName("abrir dos veces no duplica la fila ni devuelve el saldo a cero")
        void abrir_dos_veces_no_duplica_ni_resetea() {
            repository.openIfAbsent(SchemaSeed.COMPANY_ID, ABIERTO_EL);
            repository.applyDelta(SchemaSeed.COMPANY_ID, new BigDecimal("340000.00"), MOVIDO_EL);

            repository.openIfAbsent(SchemaSeed.COMPANY_ID, LocalDateTime.of(2026, 4, 1, 0, 0));

            // Si el ON DUPLICATE KEY escribiera balance_amount = 0, el saldo del
            // cliente desapareceria en silencio cada vez que alguien reabriera la
            // cuenta. Por eso la asercion es sobre el importe, no sobre el conteo.
            assertThat(repository.findByCompanyId(SchemaSeed.COMPANY_ID)).get()
                    .satisfies(saldo -> assertThat(saldo.getBalanceAmount())
                            .isEqualByComparingTo("340000.00"));
            assertThat(contarFilas(SchemaSeed.COMPANY_ID)).isEqualTo(1L);
        }

        @Test
        @DisplayName("dos filas de saldo para la misma empresa las para uq_ccb_company")
        void dos_filas_para_la_misma_empresa_las_para_la_unicidad() {
            repository.openIfAbsent(SchemaSeed.COMPANY_ID, ABIERTO_EL);
            entityManager.flush();

            // Se escribe cruda porque openIfAbsent perdona la colision a proposito; lo
            // que se comprueba aqui es que la barandilla del motor sigue puesta debajo.
            EngineConstraint.assertViolates("uq_ccb_company",
                    () -> insertarSaldoCrudo(SchemaSeed.COMPANY_ID, new BigDecimal("1.00")));
        }
    }

    @Nested
    @DisplayName("Barandilla del saldo")
    class BarandillaDelSaldo {

        @Test
        @DisplayName("un abono mueve el saldo, devuelve una fila y sube la version")
        void un_abono_mueve_el_saldo_y_sube_la_version() {
            repository.openIfAbsent(SchemaSeed.COMPANY_ID, ABIERTO_EL);
            long versionInicial = versionDe(SchemaSeed.COMPANY_ID);

            int filas = repository.applyDelta(SchemaSeed.COMPANY_ID, new BigDecimal("125000.50"),
                    MOVIDO_EL);

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findByCompanyId(SchemaSeed.COMPANY_ID)).get().satisfies(saldo -> {
                assertThat(saldo.getBalanceAmount()).isEqualByComparingTo("125000.50");
                assertThat(saldo.getRecalculatedAt()).isEqualTo(MOVIDO_EL);
                assertThat(saldo.hasCredit()).isTrue();
            });
            // Sin este incremento, un save concurrente que venga de una lectura
            // anterior casa con la version vieja y deshace el movimiento (#53).
            assertThat(versionDe(SchemaSeed.COMPANY_ID)).isEqualTo(versionInicial + 1);
        }

        @Test
        @DisplayName("un consumo que dejaria el saldo bajo cero afecta CERO filas y no toca nada")
        void un_consumo_que_dejaria_el_saldo_bajo_cero_afecta_cero_filas() {
            repository.openIfAbsent(SchemaSeed.COMPANY_ID, ABIERTO_EL);
            repository.applyDelta(SchemaSeed.COMPANY_ID, new BigDecimal("100000.00"), MOVIDO_EL);
            long versionAntes = versionDe(SchemaSeed.COMPANY_ID);

            // 100000 disponibles, se piden 100000.01: un centavo de mas. Si alguien
            // cambiara el >= por un > o comparara enteros, este caso lo caza.
            int filas = repository.applyDelta(SchemaSeed.COMPANY_ID, new BigDecimal("-100000.01"),
                    LocalDateTime.of(2026, 4, 2, 11, 0));

            assertThat(filas).isZero();
            assertThat(repository.findByCompanyId(SchemaSeed.COMPANY_ID)).get().satisfies(saldo -> {
                assertThat(saldo.getBalanceAmount()).isEqualByComparingTo("100000.00");
                // Ni siquiera la marca de recalculo se mueve: la fila no se toca.
                assertThat(saldo.getRecalculatedAt()).isEqualTo(MOVIDO_EL);
            });
            assertThat(versionDe(SchemaSeed.COMPANY_ID)).isEqualTo(versionAntes);
        }

        @Test
        @DisplayName("un consumo que deja el saldo exactamente en cero si pasa")
        void un_consumo_que_deja_el_saldo_en_cero_si_pasa() {
            repository.openIfAbsent(SchemaSeed.COMPANY_ID, ABIERTO_EL);
            repository.applyDelta(SchemaSeed.COMPANY_ID, new BigDecimal("100000.00"), MOVIDO_EL);

            int filas = repository.applyDelta(SchemaSeed.COMPANY_ID, new BigDecimal("-100000.00"),
                    LocalDateTime.of(2026, 4, 2, 11, 0));

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findByCompanyId(SchemaSeed.COMPANY_ID)).get().satisfies(
                    saldo -> assertThat(saldo.getBalanceAmount()).isEqualByComparingTo("0.00"));
        }

        @Test
        @DisplayName("mover el saldo de una empresa sin cuenta abierta afecta cero filas")
        void mover_el_saldo_de_una_empresa_sin_cuenta_afecta_cero_filas() {
            assertThat(repository.applyDelta(SchemaSeed.COMPANY_ID, new BigDecimal("50.00"),
                    MOVIDO_EL)).isZero();
            assertThat(repository.findByCompanyId(SchemaSeed.COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("el movimiento esta acotado por empresa y no arrastra el saldo vecino")
        void el_movimiento_esta_acotado_por_empresa() {
            repository.openIfAbsent(SchemaSeed.COMPANY_ID, ABIERTO_EL);
            repository.openIfAbsent(SchemaSeed.OTRA_COMPANY_ID, ABIERTO_EL);
            repository.applyDelta(SchemaSeed.COMPANY_ID, new BigDecimal("80000.00"), MOVIDO_EL);
            repository.applyDelta(SchemaSeed.OTRA_COMPANY_ID, new BigDecimal("15000.00"),
                    MOVIDO_EL);

            repository.applyDelta(SchemaSeed.COMPANY_ID, new BigDecimal("-30000.00"), MOVIDO_EL);

            // Importes distintos en las dos empresas: si el WHERE perdiera el
            // company_id, los dos saldos convergerian y la asercion caeria.
            assertThat(repository.findByCompanyId(SchemaSeed.COMPANY_ID)).get().satisfies(
                    saldo -> assertThat(saldo.getBalanceAmount()).isEqualByComparingTo("50000.00"));
            assertThat(repository.findByCompanyId(SchemaSeed.OTRA_COMPANY_ID)).get().satisfies(
                    saldo -> assertThat(saldo.getBalanceAmount()).isEqualByComparingTo("15000.00"));
        }

        @Test
        @DisplayName("el motor rechaza un saldo negativo aunque el UPDATE pierda su condicion")
        void el_motor_rechaza_un_saldo_negativo() {
            repository.openIfAbsent(SchemaSeed.COMPANY_ID, ABIERTO_EL);
            entityManager.flush();

            // Cinturon bajo el tirante: si alguien reescribiera applyDelta sin el
            // "AND balance_amount + :delta >= 0", esto es lo unico que quedaria en
            // pie. Se escribe crudo porque el UPDATE de produccion no puede llegar
            // aqui mientras conserve su condicion.
            EngineConstraint.assertViolates("chk_ccb_not_negative",
                    () -> entityManager.createNativeQuery("""
                            UPDATE customer_credit_balances
                               SET balance_amount = -0.01, version = version + 1
                             WHERE company_id = :companyId
                            """).setParameter("companyId", SchemaSeed.COMPANY_ID).executeUpdate());
        }
    }

    @Nested
    @DisplayName("Proxima caducidad y barrido")
    class ProximaCaducidadYBarrido {

        @Test
        @DisplayName("refrescar la proxima caducidad guarda la fecha y mueve marca y version")
        void refrescar_la_proxima_caducidad_guarda_la_fecha() {
            repository.openIfAbsent(SchemaSeed.COMPANY_ID, ABIERTO_EL);
            long versionAntes = versionDe(SchemaSeed.COMPANY_ID);

            repository.refreshNextExpiry(SchemaSeed.COMPANY_ID, LocalDate.of(2026, 6, 30),
                    MOVIDO_EL);

            assertThat(repository.findByCompanyId(SchemaSeed.COMPANY_ID)).get().satisfies(saldo -> {
                assertThat(saldo.getNextExpiryOn()).isEqualTo(LocalDate.of(2026, 6, 30));
                assertThat(saldo.getRecalculatedAt()).isEqualTo(MOVIDO_EL);
            });
            assertThat(versionDe(SchemaSeed.COMPANY_ID)).isEqualTo(versionAntes + 1);
        }

        @Test
        @DisplayName("refrescar a vacio borra la fecha: el ultimo lote con caducidad se gasto")
        void refrescar_a_vacio_borra_la_fecha() {
            repository.openIfAbsent(SchemaSeed.COMPANY_ID, ABIERTO_EL);
            repository.refreshNextExpiry(SchemaSeed.COMPANY_ID, LocalDate.of(2026, 6, 30),
                    MOVIDO_EL);

            repository.refreshNextExpiry(SchemaSeed.COMPANY_ID, null, MOVIDO_EL);

            assertThat(repository.findByCompanyId(SchemaSeed.COMPANY_ID)).get()
                    .satisfies(saldo -> assertThat(saldo.getNextExpiryOn()).isNull());
        }

        @Test
        @DisplayName("el barrido de plataforma trae primero el saldo mas alto")
        void el_barrido_trae_primero_el_saldo_mas_alto() {
            repository.openIfAbsent(SchemaSeed.COMPANY_ID, ABIERTO_EL);
            repository.openIfAbsent(SchemaSeed.OTRA_COMPANY_ID, ABIERTO_EL);
            repository.applyDelta(SchemaSeed.COMPANY_ID, new BigDecimal("12000.00"), MOVIDO_EL);
            repository.applyDelta(SchemaSeed.OTRA_COMPANY_ID, new BigDecimal("990000.00"),
                    MOVIDO_EL);

            assertThat(repository.findAll(0, 20).content())
                    .extracting(CustomerCreditBalance::getCompanyId)
                    .containsExactly(SchemaSeed.OTRA_COMPANY_ID, SchemaSeed.COMPANY_ID);
        }
    }

    // --- andamio ------------------------------------------------------------

    private long versionDe(Long companyId) {
        return ((Number) entityManager.createNativeQuery(
                "SELECT version FROM customer_credit_balances WHERE company_id = :companyId")
                .setParameter("companyId", companyId).getSingleResult()).longValue();
    }

    private long contarFilas(Long companyId) {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM customer_credit_balances"
                        + " WHERE company_id = :companyId")
                .setParameter("companyId", companyId).getSingleResult()).longValue();
    }

    private void insertarSaldoCrudo(Long companyId, BigDecimal importe) {
        entityManager.createNativeQuery("""
                INSERT INTO customer_credit_balances
                        (company_id, balance_amount, next_expiry_on, recalculated_at, version)
                VALUES (:companyId, :importe, NULL, :cuando, 0)
                """).setParameter("companyId", companyId).setParameter("importe", importe)
                .setParameter("cuando", MOVIDO_EL).executeUpdate();
    }
}
