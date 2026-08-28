package com.vetsoftware.app.accountingaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.accountingaccount.domain.AccountClass;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccount;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaAccountingAccountRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo primero que vigila esta clase es el tipo de {@code account_level}</b>,
 * y no es una comprobacion academica: la columna es {@code TINYINT} y el campo
 * de la entidad tiene que ser {@code byte}. Con un {@code int} ahi,
 * {@code ddl-auto: validate} no falla en esta rodaja — impide construir el
 * {@code SessionFactory} y <b>ningun contexto de Spring del repositorio
 * arranca</b>. Es un fallo de una linea con radio de accion de suite entera, y
 * este ida y vuelta es lo que lo caza el mismo dia.
 *
 * <p>
 * <b>Lo segundo son las dos invariantes que sostienen el balance de prueba</b>
 * y que ningun test de dominio puede demostrar en la base:
 * {@code chk_accounting_accounts_postable} —solo el nivel 6 admite asiento— y
 * {@code chk_accounting_accounts_parent} con sus dos ramas. Se escriben por SQL
 * nativo a proposito, saltandose el constructor: es la unica forma de comprobar
 * que el cinturon existe debajo del tirante.
 *
 * <p>
 * <b>Por que el adaptador se construye a mano.</b>
 * {@code PersistenceSliceConfig} reune los adaptadores de las rodajas para que
 * todas compartan una clave de contexto y, con ella, un unico contexto
 * cacheado. Declarar aqui un {@code @Import} propio con este adaptador le daria
 * a esta clase una clave unica y un arranque de contexto entero para ella sola.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaAccountingAccountRepository — el plan de cuentas contra MySQL real")
class AccountingAccountPersistenceIT extends AbstractDataJpaTest {

    /** Id del rango reservado a esta rodaja. */
    private static final Long CUENTA_CRUDA = 8400L;

    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDateTime CREADA_EL = LocalDateTime.of(2026, 1, 1, 8, 0, 0);

    @Autowired
    private AccountingAccountJpaRepository springDataRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaAccountingAccountRepository repository;

    @BeforeEach
    void adaptador() {
        repository = new JpaAccountingAccountRepository(springDataRepository,
                new AccountingAccountJpaMapper());
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda la subcuenta y la recupera con cada campo en su sitio")
        void guarda_la_subcuenta_y_la_recupera_campo_a_campo() {
            repository.save(raiz());
            AccountingAccount guardada = repository.save(subcuenta("11050501", "Bancos"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(recuperada -> {
                assertThat(recuperada.getCode()).isEqualTo("11050501");
                assertThat(recuperada.getName()).isEqualTo("Bancos");
                assertThat(recuperada.getAccountClass()).isEqualTo(AccountClass.ASSET);
                assertThat(recuperada.getParentCode()).isEqualTo("1");
                // EL caso: el nivel viaja por una columna TINYINT y vuelve intacto.
                assertThat(recuperada.getAccountLevel()).isEqualTo(6);
                assertThat(recuperada.isPostable()).isTrue();
                assertThat(recuperada.isRequiresThirdParty()).isFalse();
                assertThat(recuperada.getValidFrom()).isEqualTo(DESDE);
                assertThat(recuperada.getValidTo()).isNull();
                assertThat(recuperada.isEnabled()).isTrue();
                assertThat(recuperada.getVersion()).isNotNull();
            });
        }

        @Test
        @DisplayName("cerrar la vigencia mueve la version: es una edicion, no un insert")
        void cerrar_la_vigencia_mueve_la_version() {
            repository.save(raiz());
            AccountingAccount guardada = repository.save(subcuenta("11050502", "Caja"));
            entityManager.flush();
            entityManager.clear();

            AccountingAccount cargada = repository.findById(guardada.getId()).orElseThrow();
            repository.save(cargada.close(LocalDate.of(2027, 1, 1)));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(cerrada -> {
                assertThat(cerrada.getValidTo()).isEqualTo(LocalDate.of(2027, 1, 1));
                assertThat(cerrada.isOpen()).isFalse();
                // La version se movio: el UPDATE paso por el ciclo de Hibernate y no
                // por una escritura masiva que la dejaria intacta.
                assertThat(cerrada.getVersion()).isEqualTo(1L);
            });
        }

        @Test
        @DisplayName("el plan se lista por codigo, que es su orden jerarquico")
        void el_plan_se_lista_por_codigo() {
            repository.save(raiz());
            repository.save(subcuenta("11050504", "Cuatro"));
            repository.save(subcuenta("11050503", "Tres"));
            entityManager.flush();
            entityManager.clear();

            PageResult<AccountingAccount> pagina = repository.findAllEnabled(0, 20);

            assertThat(pagina.content()).extracting(AccountingAccount::getCode).containsExactly("1",
                    "11050503", "11050504");
        }
    }

    @Nested
    @DisplayName("Unicidad del codigo")
    class UnicidadDelCodigo {

        @Test
        @DisplayName("el mismo codigo dos veces lo para uq_accounting_accounts_code")
        void el_mismo_codigo_dos_veces_lo_para_la_unicidad() {
            repository.save(raiz());
            repository.save(subcuenta("11050505", "Primera"));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_accounting_accounts_code", () -> {
                repository.save(subcuenta("11050505", "Segunda"));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("la comprobacion previa distingue el codigo que existe del que no")
        void la_comprobacion_previa_distingue_el_codigo_que_existe() {
            repository.save(raiz());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.existsByCode("1")).isTrue();
            assertThat(repository.existsByCode("9")).isFalse();
        }

        @Test
        @DisplayName("existsPostable distingue la cuenta asentable del grupo")
        void exists_postable_distingue_la_cuenta_asentable() {
            // Es la consulta que account_mappings usa antes de publicar un mapeo:
            // la raiz existe pero NO admite asiento, y la subcuenta si.
            repository.save(raiz());
            repository.save(subcuenta("11050506", "Asentable"));
            entityManager.flush();
            entityManager.clear();

            assertThat(springDataRepository.existsByCodeAndPostableTrue("11050506")).isTrue();
            assertThat(springDataRepository.existsByCodeAndPostableTrue("1")).isFalse();
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("asentar contra un grupo lo para chk_accounting_accounts_postable")
        void asentar_contra_un_grupo_lo_para_el_check() {
            // El dominio ya lo rechaza, asi que la unica forma de comprobar que la base
            // tambien lo hace es escribir la fila por SQL nativo. Sin esta barandilla se
            // asienta contra un grupo y el balance de prueba deja de cuadrar por
            // arrastre, sin un solo error.
            EngineConstraint.assertViolates("chk_accounting_accounts_postable",
                    () -> insertarCruda(CUENTA_CRUDA, "9001", "ASSET", "1", 2, true));
        }

        @Test
        @DisplayName("una subcuenta huerfana la para chk_accounting_accounts_parent")
        void una_subcuenta_huerfana_la_para_el_check_del_padre() {
            // La segunda rama del CHECK. Sin ella, un nivel 6 sin padre entraria en
            // silencio: NULL no esta "fuera de la lista", esta indefinido.
            EngineConstraint.assertViolates("chk_accounting_accounts_parent",
                    () -> insertarCruda(CUENTA_CRUDA + 1, "9002", "ASSET", null, 6, true));
        }

        @Test
        @DisplayName("un nivel fuera de (1,2,4,6) lo para chk_accounting_accounts_level")
        void un_nivel_desconocido_lo_para_el_check_del_nivel() {
            EngineConstraint.assertViolates("chk_accounting_accounts_level",
                    () -> insertarCruda(CUENTA_CRUDA + 2, "9003", "ASSET", "1", 3, false));
        }

        @Test
        @DisplayName("una clase de cuenta desconocida la para chk_accounting_accounts_class")
        void una_clase_desconocida_la_para_el_check_de_clase() {
            EngineConstraint.assertViolates("chk_accounting_accounts_class",
                    () -> insertarCruda(CUENTA_CRUDA + 3, "9004", "PATRIMONIO", "1", 6, false));
        }
    }

    /** La raiz del plan: nivel 1, sin padre y sin asiento. */
    private static AccountingAccount raiz() {
        return AccountingAccount.create("1", "Activo", AccountClass.ASSET, null, 1, false, false,
                DESDE, null, CREADA_EL);
    }

    private static AccountingAccount subcuenta(String codigo, String nombre) {
        return AccountingAccount.create(codigo, nombre, AccountClass.ASSET, "1", 6, true, false,
                DESDE, null, CREADA_EL);
    }

    private void insertarCruda(Long id, String codigo, String clase, String padre, int nivel,
            boolean asentable) {
        entityManager.createNativeQuery("""
                INSERT INTO accounting_accounts (id, code, name, account_class, parent_code,
                        account_level, postable, requires_third_party, valid_from, created_date,
                        enabled, version)
                VALUES (:id, :codigo, 'Cuenta cruda', :clase, :padre, :nivel, :asentable, false,
                        '2026-01-01', NOW(6), true, 0)
                """).setParameter("id", id).setParameter("codigo", codigo)
                .setParameter("clase", clase).setParameter("padre", padre)
                .setParameter("nivel", nivel).setParameter("asentable", asentable).executeUpdate();
        entityManager.flush();
    }
}
