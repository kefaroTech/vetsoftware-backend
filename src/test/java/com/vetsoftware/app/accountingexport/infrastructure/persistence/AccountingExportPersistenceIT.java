package com.vetsoftware.app.accountingexport.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.accountingexport.domain.AccountingExport;
import com.vetsoftware.app.accountingexport.domain.AccountingExportKind;
import com.vetsoftware.app.accountingexport.domain.AccountingExportStatus;
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
 * Rodaja de {@code JpaAccountingExportRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar son las dos unicidades que hacen que
 * «rehacer un fichero» signifique algo</b>, y una de ellas cuelga de una
 * columna generada que no se ve desde Java:
 *
 * <ul>
 * <li>{@code uq_accounting_exports_attempt} impide exportar dos veces el mismo
 * intento del mismo mes y clase.</li>
 * <li>{@code uq_accounting_exports_current}, via {@code current_export_marker},
 * impide <b>dos ficheros vivos</b> del mismo mes y clase. No estaba en el
 * documento maestro, y sin ella el contador acaba con dos ficheros del mismo
 * mes sin saber cual vale.
 * {@link Unicidad#rechazar_el_fichero_libera_el_hueco()} es la otra mitad: el
 * marcador se vuelve {@code NULL} y el siguiente intento cabe.</li>
 * </ul>
 *
 * <p>
 * <b>Y comprueba la partida doble sobre dos numeros</b>
 * ({@code chk_accounting_exports_balanced}), que es la invariante contable que
 * el documento daba por imposible de imponer y que al eliminar el diario pasa a
 * ser trivial.
 *
 * <p>
 * <b>El seed no trae periodos contables.</b> El de <b>2028-05</b> se inserta
 * aqui por SQL nativo —clave y rango de ids que ninguna otra rodaja usa— y va
 * {@code OPEN} porque el changeset 346 pone un disparador que impide escribir
 * una exportacion contra un mes cerrado.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaAccountingExportRepository — el asiento resumen contra MySQL real")
class AccountingExportPersistenceIT extends AbstractDataJpaTest {

    private static final Long PERIODO_ID = 8430L;
    private static final Long EXPORTACION_CRUDA = 8431L;

    private static final String MAYO = "2028-05";

    /**
     * SHA-256 de sesenta y cuatro hexadecimales en minusculas, como exige el CHECK.
     */
    private static final String HUELLA = "a".repeat(64);

    private static final LocalDateTime GENERADA_EL = LocalDateTime.of(2028, 6, 1, 3, 0, 0);

    @Autowired
    private AccountingExportJpaRepository springDataRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaAccountingExportRepository repository;

    @BeforeEach
    void adaptador() {
        SchemaSeed.seed(entityManager);
        entityManager.createNativeQuery("""
                INSERT INTO accounting_periods (id, period_key, status, created_date, version)
                VALUES (:id, :clave, 'OPEN', NOW(6), 0)
                """).setParameter("id", PERIODO_ID).setParameter("clave", MAYO).executeUpdate();
        entityManager.flush();
        repository = new JpaAccountingExportRepository(springDataRepository,
                new AccountingExportJpaMapper());
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda la exportacion y la recupera con cada campo en su sitio")
        void guarda_la_exportacion_y_la_recupera_campo_a_campo() {
            AccountingExport guardada = repository.save(generada(1));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(recuperada -> {
                assertThat(recuperada.getPeriodKey()).isEqualTo(MAYO);
                assertThat(recuperada.getExportKind())
                        .isEqualTo(AccountingExportKind.JOURNAL_SUMMARY);
                assertThat(recuperada.getAttemptNumber()).isEqualTo(1);
                assertThat(recuperada.getStatus()).isEqualTo(AccountingExportStatus.GENERATED);
                assertThat(recuperada.getGeneratedBySystemUserId())
                        .isEqualTo(SchemaSeed.SYSTEM_USER_ID);
                assertThat(recuperada.getTotalDebit()).isEqualByComparingTo("980000.00");
                assertThat(recuperada.getTotalCredit()).isEqualByComparingTo("980000.00");
                // La huella viaja por una columna CHAR(64) y vuelve entera: es lo que
                // permite demostrar que el fichero del contador es el que se genero.
                assertThat(recuperada.getTotalsHash()).isEqualTo(HUELLA);
                assertThat(recuperada.getDeliveredAt()).isNull();
                assertThat(recuperada.isCurrent()).isTrue();
                assertThat(recuperada.getVersion()).isNotNull();
            });
        }

        @Test
        @DisplayName("entregar el fichero mueve la version: es una edicion, no un insert")
        void entregar_el_fichero_mueve_la_version() {
            AccountingExport guardada = repository.save(generada(1));
            entityManager.flush();
            entityManager.clear();

            AccountingExport cargada = repository.findById(guardada.getId()).orElseThrow();
            repository.save(cargada.markDelivered(GENERADA_EL.plusDays(1)));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(entregada -> {
                assertThat(entregada.getStatus()).isEqualTo(AccountingExportStatus.DELIVERED);
                assertThat(entregada.getDeliveredAt()).isEqualTo(GENERADA_EL.plusDays(1));
                assertThat(entregada.getVersion()).isEqualTo(1L);
            });
        }

        @Test
        @DisplayName("el ultimo intento se resuelve por el extremo del indice")
        void el_ultimo_intento_se_resuelve_por_el_indice() {
            // Es lo que el service consulta para calcular el intento siguiente sin
            // pedirselo al llamador.
            AccountingExport primera = repository.save(generada(1));
            entityManager.flush();
            entityManager.clear();

            repository.save(repository.findById(primera.getId()).orElseThrow()
                    .markRejected(GENERADA_EL.plusDays(1), "Faltan terceros"));
            repository.save(generada(2));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findLastAttemptNumber(MAYO, AccountingExportKind.JOURNAL_SUMMARY))
                    .contains(2);
            assertThat(repository.findLastAttemptNumber(MAYO, AccountingExportKind.VAT_SUPPORT))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Unicidad")
    class Unicidad {

        @Test
        @DisplayName("dos ficheros vivos del mismo mes y clase no caben")
        void dos_ficheros_vivos_del_mismo_mes_y_clase_no_caben() {
            repository.save(generada(1));
            entityManager.flush();

            // El intento es distinto, asi que uq_accounting_exports_attempt no los ve
            // iguales; lo que los para es current_export_marker.
            EngineConstraint.assertViolates("uq_accounting_exports_current", () -> {
                repository.save(generada(2));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("rechazar el fichero libera el hueco para el intento siguiente")
        void rechazar_el_fichero_libera_el_hueco() {
            AccountingExport primera = repository.save(generada(1));
            entityManager.flush();
            entityManager.clear();

            repository.save(repository.findById(primera.getId()).orElseThrow()
                    .markRejected(GENERADA_EL.plusDays(1), "Faltan terceros"));
            entityManager.flush();

            AccountingExport segunda = repository.save(generada(2));
            entityManager.flush();

            assertThat(segunda.getId()).isNotNull();
            assertThat(segunda.getAttemptNumber()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("un asiento descuadrado lo para chk_accounting_exports_balanced")
        void un_asiento_descuadrado_lo_para_el_check_de_partida_doble() {
            // La invariante contable que el documento maestro daba por imposible de
            // imponer. El dominio ya la comprueba; esto es el cinturon de debajo.
            EngineConstraint.assertViolates("chk_accounting_exports_balanced",
                    () -> insertarCruda(EXPORTACION_CRUDA, 9, "GENERATED", "980000.00", "970000.00",
                            HUELLA));
        }

        @Test
        @DisplayName("una huella que no es SHA-256 la para chk_accounting_exports_hash")
        void una_huella_invalida_la_para_el_check_de_la_huella() {
            EngineConstraint.assertViolates("chk_accounting_exports_hash",
                    () -> insertarCruda(EXPORTACION_CRUDA + 1, 10, "GENERATED", "980000.00",
                            "980000.00", "NO-ES-UNA-HUELLA"));
        }

        @Test
        @DisplayName("un estado desconocido lo para el motor")
        void un_estado_desconocido_lo_para_el_motor() {
            // El vocabulario esta vigilado dos veces: chk_..._status por la lista y
            // chk_..._lifecycle porque un valor desconocido no cae en ninguna de sus
            // cuatro ramas. La que dispara primero es la del ciclo de vida.
            EngineConstraint.assertViolates("chk_accounting_exports_lifecycle",
                    () -> insertarCruda(EXPORTACION_CRUDA + 2, 11, "PENDING", "980000.00",
                            "980000.00", HUELLA));
        }
    }

    private static AccountingExport generada(int intento) {
        return AccountingExport.generate(MAYO, AccountingExportKind.JOURNAL_SUMMARY, intento,
                GENERADA_EL, SchemaSeed.SYSTEM_USER_ID, new BigDecimal("980000.00"),
                new BigDecimal("980000.00"), HUELLA, "s3://contable/2028-05.csv", GENERADA_EL);
    }

    private void insertarCruda(Long id, int intento, String estado, String debito, String credito,
            String huella) {
        entityManager.createNativeQuery("""
                INSERT INTO accounting_exports (id, period_key, export_kind, attempt_number,
                        status, generated_at, generated_by_system_user_id, total_debit,
                        total_credit, totals_hash, file_ref, created_date, version)
                VALUES (:id, :clave, 'JOURNAL_SUMMARY', :intento, :estado, NOW(6), :usuario,
                        :debito, :credito, :huella, 's3://contable/crudo.csv', NOW(6), 0)
                """).setParameter("id", id).setParameter("clave", MAYO)
                .setParameter("intento", intento).setParameter("estado", estado)
                .setParameter("usuario", SchemaSeed.SYSTEM_USER_ID)
                .setParameter("debito", new BigDecimal(debito))
                .setParameter("credito", new BigDecimal(credito)).setParameter("huella", huella)
                .executeUpdate();
        entityManager.flush();
    }
}
