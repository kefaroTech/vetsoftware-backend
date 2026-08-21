package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
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
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia de los abonos contra MySQL real.
 *
 * <p>
 * Dos cosas de este adaptador solo existen en la base. La primera es el scope
 * de empresa: el abono no tiene {@code company_id} propio, se alcanza navegando
 * {@code openAccount.company.id}, asi que el aislamiento entre tenants depende
 * de que ese salto este en cada consulta y no de ninguna comprobacion en Java.
 * La segunda es la idempotencia: el {@code clientRequestId} lo deduplica un
 * indice unico ({@code uq_debt_open_accounts_request}), que es lo que cierra la
 * carrera que el {@code findBy...ClientRequestId} del servicio deja abierta
 * —dos POST simultaneos del mismo cobro pasan los dos el chequeo previo—.
 *
 * <p>
 * Con un doble del repositorio ninguna de las dos se puede falsear: el doble
 * responderia lo que el propio test le hubiera dicho.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaDebtOpenAccountRepository — scope por cuenta e idempotencia contra MySQL real")
class DebtOpenAccountPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;
    private static final Long BRANCH_AJENA = 912L;
    private static final Long EMPLEADO = SchemaSeed.EMPLOYEE_ID;
    private static final Long OTRO_EMPLEADO = SchemaSeed.OTRO_EMPLOYEE_ID;

    private static final Long OWNER = 960L;
    private static final Long OWNER_AJENO = 962L;
    private static final Long CUENTA = 970L;
    private static final Long OTRA_CUENTA = 971L;
    private static final Long CUENTA_AJENA = 972L;

    private static final OpenAccountRef LA_CUENTA = new OpenAccountRef(CUENTA, COMPANY);
    private static final OpenAccountRef LA_OTRA_CUENTA = new OpenAccountRef(OTRA_CUENTA, COMPANY);
    private static final OpenAccountRef LA_CUENTA_AJENA = new OpenAccountRef(CUENTA_AJENA,
            OTRA_COMPANY);
    private static final EmployeeRef CAJERA = new EmployeeRef(EMPLEADO, "Ana Ruiz");
    private static final EmployeeRef SUPERVISOR = new EmployeeRef(OTRO_EMPLEADO, "Luis Paz");

    private static final LocalDateTime COBRADO = LocalDateTime.of(2026, 1, 15, 11, 0);

    @Autowired
    private JpaDebtOpenAccountRepository repository;

    @Autowired
    private JpaOpenAccountQueryPort openAccountQueryPort;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        sedeAjena();
        propietario(OWNER, "Marta Diaz", "CC-1001", COMPANY);
        propietario(OWNER_AJENO, "Ana Ajena", "CC-2001", OTRA_COMPANY);
        cuenta(CUENTA, OWNER, COMPANY, BRANCH);
        cuenta(OTRA_CUENTA, OWNER, COMPANY, SchemaSeed.OTRA_BRANCH_ID);
        cuenta(CUENTA_AJENA, OWNER_AJENO, OTRA_COMPANY, BRANCH_AJENA);
        entityManager.flush();
    }

    private void sedeAjena() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO branches (id, name, code, city_id, company_id)
                VALUES (:id, 'Sede ajena', 'AJENA', :ciudad, :empresa)
                """).setParameter("id", BRANCH_AJENA).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", OTRA_COMPANY).executeUpdate();
    }

    private void propietario(Long id, String nombre, String documento, Long companyId) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO owners (id, name, document, document_type, person_type,
                                           withholding_agent, tax_regime, fiscal_responsibility,
                                           city_id, company_id, created_date, enabled)
                VALUES (:id, :nombre, :documento, 'CEDULA_CIUDADANIA', 'NATURAL', false,
                        'NO_RESPONSABLE_IVA', 'NO_APLICA', :ciudad, :empresa,
                        '2026-01-01 08:00:00', true)
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("documento", documento).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", companyId).executeUpdate();
    }

    private void cuenta(Long id, Long ownerId, Long companyId, Long branchId) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO open_accounts (id, total_amount, paid_amount,
                                                  outstanding_amount, owner_id, company_id,
                                                  branch_id, created_by_id, created_date, enabled,
                                                  status, reversed, version)
                VALUES (:id, 0.00, 0.00, 0.00, :owner, :empresa, :sede, :empleado,
                        '2026-01-15 09:00:00', true, 'OPEN', false, 0)
                """).setParameter("id", id).setParameter("owner", ownerId)
                .setParameter("empresa", companyId).setParameter("sede", branchId)
                .setParameter("empleado", EMPLEADO).executeUpdate();
    }

    private DebtOpenAccount abono(OpenAccountRef cuenta, String importe, PaymentMethod metodo,
            String clave) {
        return repository.save(new DebtOpenAccount(null, new BigDecimal(importe), metodo, cuenta,
                CAJERA, COBRADO, null, true, false, null, null, null, clave));
    }

    private DebtOpenAccount abonoEnEfectivo() {
        return abono(LA_CUENTA, "25000.00", PaymentMethod.CASH, null);
    }

    @Nested
    @DisplayName("ida y vuelta del abono")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y el abono vuelve entero")
        void guardar_asigna_id_y_el_abono_vuelve_entero() {
            DebtOpenAccount guardado = abonoEnEfectivo();

            assertThat(guardado.getId()).isNotNull();

            DebtOpenAccount leido = repository.findByIdAndCompanyId(guardado.getId(), COMPANY)
                    .orElseThrow();
            assertThat(leido.getAmount()).isEqualByComparingTo("25000.00");
            assertThat(leido.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
            assertThat(leido.getCreatedBy().name()).isEqualTo("Ana Ruiz");
            assertThat(leido.getCreatedDate()).isEqualTo(COBRADO);
            assertThat(leido.isEnabled()).isTrue();
            assertThat(leido.isVoided()).isFalse();
            // El companyId del ref sale de la cuenta, no del abono: es el unico camino por
            // el que un abono sabe de que empresa es.
            assertThat(leido.getOpenAccount().companyId()).isEqualTo(COMPANY);
        }

        @Test
        @DisplayName("el importe conserva los dos decimales")
        void el_importe_conserva_los_dos_decimales() {
            DebtOpenAccount guardado = abono(LA_CUENTA, "12345.67", PaymentMethod.CARD, null);

            assertThat(repository.findById(guardado.getId()).orElseThrow().getAmount())
                    .isEqualByComparingTo("12345.67");
        }

        @Test
        @DisplayName("la anulacion vuelve con quien, cuando y por que")
        void la_anulacion_vuelve_con_quien_cuando_y_por_que() {
            DebtOpenAccount guardado = abonoEnEfectivo();
            DebtOpenAccount recuperado = repository.findById(guardado.getId()).orElseThrow();
            recuperado.voidPayment(SUPERVISOR, "Cobro duplicado");
            repository.save(recuperado);

            DebtOpenAccount leido = repository.findByIdAndCompanyId(guardado.getId(), COMPANY)
                    .orElseThrow();

            // Anular deja la fila visible (no toca enabled): el abono sigue en el detalle
            // con su motivo, y lo que cambia es que deja de sumar como pagado.
            assertThat(leido.isVoided()).isTrue();
            assertThat(leido.isEnabled()).isTrue();
            assertThat(leido.getVoidedBy().name()).isEqualTo("Luis Paz");
            assertThat(leido.getVoidReason()).isEqualTo("Cobro duplicado");
            assertThat(leido.getVoidedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa a traves de la cuenta")
    class Tenancy {

        @Test
        @DisplayName("un abono de otra empresa no se lee por id")
        void un_abono_de_otra_empresa_no_se_lee_por_id() {
            DebtOpenAccount ajeno = abono(LA_CUENTA_AJENA, "9000.00", PaymentMethod.CASH, null);

            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), COMPANY)).isEmpty();
            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), OTRA_COMPANY)).isPresent();
        }

        @Test
        @DisplayName("el listado por empresa no ve los abonos ajenos")
        void el_listado_por_empresa_no_ve_los_abonos_ajenos() {
            DebtOpenAccount propio = abonoEnEfectivo();
            abono(LA_CUENTA_AJENA, "9000.00", PaymentMethod.CASH, null);

            PageResult<DebtOpenAccount> pagina = repository.findAllByCompanyId(COMPANY, 0, 20);

            assertThat(pagina.content()).extracting(DebtOpenAccount::getId)
                    .containsExactly(propio.getId());
            assertThat(pagina.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("el detalle de una cuenta ajena no se lista pidiendola con la empresa propia")
        void el_detalle_de_una_cuenta_ajena_no_se_lista() {
            abono(LA_CUENTA_AJENA, "9000.00", PaymentMethod.CASH, null);

            // El id de la cuenta viaja en la URL: sin el segundo filtro por empresa,
            // pedir /by-open-account/{ajena} devolveria la cartera del otro tenant.
            assertThat(repository.findByOpenAccountIdAndCompanyId(CUENTA_AJENA, COMPANY)).isEmpty();
            assertThat(repository.findByOpenAccountIdAndCompanyId(CUENTA_AJENA, OTRA_COMPANY))
                    .hasSize(1);
        }

        @Test
        @DisplayName("el detalle de una cuenta solo trae los abonos de esa cuenta")
        void el_detalle_de_una_cuenta_solo_trae_sus_abonos() {
            DebtOpenAccount deLaCuenta = abonoEnEfectivo();
            abono(LA_OTRA_CUENTA, "1000.00", PaymentMethod.CASH, null);

            assertThat(repository.findByOpenAccountIdAndCompanyId(CUENTA, COMPANY))
                    .extracting(DebtOpenAccount::getId).containsExactly(deLaCuenta.getId());
        }

        /**
         * {@code findAll()} (sin sufijo de empresa) es el unico metodo del puerto que
         * NO acota por tenant: ningun caso de uso lo invoca hoy, pero el contrato sigue
         * expuesto y hay que documentar su alcance real, no asumirlo.
         */
        @Test
        @DisplayName("findAll (sin sufijo de empresa) trae abonos de todas las empresas")
        void find_all_sin_sufijo_trae_abonos_de_todas_las_empresas() {
            DebtOpenAccount propio = abonoEnEfectivo();
            DebtOpenAccount ajeno = abono(LA_CUENTA_AJENA, "9000.00", PaymentMethod.CASH, null);

            assertThat(repository.findAll()).extracting(DebtOpenAccount::getId)
                    .containsExactlyInAnyOrder(propio.getId(), ajeno.getId());
        }
    }

    @Nested
    @DisplayName("idempotencia del cobro")
    class Idempotencia {

        @Test
        @DisplayName("encuentra el abono ya registrado con esa clave")
        void encuentra_el_abono_ya_registrado_con_esa_clave() {
            DebtOpenAccount guardado = abono(LA_CUENTA, "25000.00", PaymentMethod.CASH,
                    "8f14e45f-ea01-4d0a-9c1a-000000000001");

            assertThat(repository.findByOpenAccountIdAndClientRequestId(CUENTA,
                    "8f14e45f-ea01-4d0a-9c1a-000000000001")).map(DebtOpenAccount::getId)
                    .contains(guardado.getId());
        }

        @Test
        @DisplayName("el finder por clave NO acota empresa: la barrera es el orden del servicio")
        void el_finder_por_clave_no_acota_por_empresa() {
            DebtOpenAccount ajeno = abono(LA_CUENTA_AJENA, "25000.00", PaymentMethod.CASH,
                    "8f14e45f-ea01-4d0a-9c1a-000000000009");

            // La fila del abono no tiene company_id y esta consulta no navega a
            // open_accounts: con el id de una cuenta AJENA y la clave exacta devuelve el
            // abono del otro tenant. Por eso el servicio resuelve la cuenta ACOTADA antes
            // de llamar aqui —ese orden es toda la barrera— y este test es la razon por la
            // que no se puede volver a invertir.
            assertThat(repository.findByOpenAccountIdAndClientRequestId(CUENTA_AJENA,
                    "8f14e45f-ea01-4d0a-9c1a-000000000009")).map(DebtOpenAccount::getId)
                    .contains(ajeno.getId());
        }

        @Test
        @DisplayName("la misma clave en otra cuenta no es el mismo cobro")
        void la_misma_clave_en_otra_cuenta_no_es_el_mismo_cobro() {
            abono(LA_CUENTA, "25000.00", PaymentMethod.CASH,
                    "8f14e45f-ea01-4d0a-9c1a-000000000001");

            // La clave se deduplica POR cuenta: el indice unico es (open_account_id,
            // client_request_id). Si fuera global, un uuid repetido entre clientes
            // bloquearia un cobro legitimo.
            assertThat(repository.findByOpenAccountIdAndClientRequestId(OTRA_CUENTA,
                    "8f14e45f-ea01-4d0a-9c1a-000000000001")).isEmpty();
        }

        @Test
        @DisplayName("dos abonos con la misma clave en la misma cuenta los corta la base")
        void dos_abonos_con_la_misma_clave_los_corta_la_base() {
            abono(LA_CUENTA, "25000.00", PaymentMethod.CASH,
                    "8f14e45f-ea01-4d0a-9c1a-000000000001");
            entityManager.flush();

            // Es la mitad que el chequeo previo del servicio no puede cubrir: dos POST
            // simultaneos lo pasan los dos y solo el indice unico impide cobrar dos veces.
            assertThatThrownBy(() -> {
                abono(LA_CUENTA, "25000.00", PaymentMethod.CASH,
                        "8f14e45f-ea01-4d0a-9c1a-000000000001");
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class)
                    .hasStackTraceContaining("uq_debt_open_accounts_request");
        }

        @Test
        @DisplayName("varios abonos sin clave conviven: el indice unico admite muchos NULL")
        void varios_abonos_sin_clave_conviven() {
            abonoEnEfectivo();
            abonoEnEfectivo();
            entityManager.flush();

            // El cliente antiguo no manda clave. Si el indice no tolerara los NULL, la
            // segunda cuota del mismo cliente seria imposible de registrar.
            assertThat(repository.findByOpenAccountIdAndCompanyId(CUENTA, COMPANY)).hasSize(2);
        }
    }

    @Nested
    @DisplayName("listado paginado")
    class Listado {

        @Test
        @DisplayName("devuelve primero lo mas reciente y con los metadatos de la consulta")
        void devuelve_primero_lo_mas_reciente_con_sus_metadatos() {
            DebtOpenAccount primero = abonoEnEfectivo();
            DebtOpenAccount segundo = abono(LA_CUENTA, "1000.00", PaymentMethod.BANK_TRANSFER,
                    null);

            PageResult<DebtOpenAccount> pagina = repository.findAllByCompanyId(COMPANY, 0, 1);

            assertThat(pagina.content()).extracting(DebtOpenAccount::getId)
                    .containsExactly(segundo.getId());
            assertThat(pagina.totalElements()).isEqualTo(2L);
            assertThat(pagina.totalPages()).isEqualTo(2);
            assertThat(repository.findAllByCompanyId(COMPANY, 1, 1).content())
                    .extracting(DebtOpenAccount::getId).containsExactly(primero.getId());
        }

        @Test
        @DisplayName("todos los metodos de pago sobreviven al viaje")
        void todos_los_metodos_de_pago_sobreviven_al_viaje() {
            abono(LA_CUENTA, "1.00", PaymentMethod.CASH, null);
            abono(LA_CUENTA, "2.00", PaymentMethod.CARD, null);
            abono(LA_CUENTA, "3.00", PaymentMethod.BANK_TRANSFER, null);

            // La columna es VARCHAR(20) con el nombre del enum: si alguien anade un metodo
            // mas largo, esto es lo que lo detecta antes de produccion.
            assertThat(repository.findByOpenAccountIdAndCompanyId(CUENTA, COMPANY))
                    .extracting(DebtOpenAccount::getPaymentMethod).containsExactlyInAnyOrder(
                            PaymentMethod.CASH, PaymentMethod.CARD, PaymentMethod.BANK_TRANSFER);
        }
    }

    /**
     * El otro adaptador de esta feature: el que resuelve la cuenta destino del
     * abono. Se prueba aqui porque la semilla ya tiene dos empresas con cuenta
     * propia, y porque lo que hay que ver es la consulta —el
     * {@code findByIdAndCompany_Id} de {@code open_accounts}— y no un doble que
     * responderia lo que el test le diga.
     */
    @Nested
    @DisplayName("la cuenta destino se resuelve acotada por empresa")
    class CuentaDestinoAcotada {

        @Test
        @DisplayName("la cuenta propia se resuelve con la empresa de su fila")
        void la_cuenta_propia_se_resuelve() {
            assertThat(openAccountQueryPort.findByIdAndCompanyId(CUENTA, COMPANY))
                    .contains(LA_CUENTA);
        }

        @Test
        @DisplayName("la cuenta de otra empresa no se resuelve: no hay donde colgar el abono")
        void la_cuenta_de_otra_empresa_no_se_resuelve() {
            assertThat(openAccountQueryPort.findByIdAndCompanyId(CUENTA_AJENA, COMPANY)).isEmpty();
            assertThat(openAccountQueryPort.findByIdAndCompanyId(CUENTA_AJENA, OTRA_COMPANY))
                    .contains(LA_CUENTA_AJENA);
            // La variante ancha SI la devuelve: era la puerta por la que un abono
            // terminaba en la cuenta de un cliente del otro tenant.
            assertThat(openAccountQueryPort.findById(CUENTA_AJENA)).contains(LA_CUENTA_AJENA);
        }

        @Test
        @DisplayName("el bloqueo pesimista tambien va acotado y se ejecuta contra MySQL")
        void el_bloqueo_pesimista_va_acotado() {
            // El FOR UPDATE se ejecuta de verdad aqui: la version ancha tomaba el
            // PESSIMISTIC_WRITE sobre la fila del OTRO tenant antes de cualquier
            // comprobacion. Que el lock se conceda o no solo se ve desde una segunda
            // conexion —eso ya lo cubre OpenAccountPersistenceIT sobre la consulta
            // acotada—; lo que se fija aqui es que el adaptador cuelga de esa consulta y
            // que la cuenta ajena no devuelve fila, asi que no hay nada que bloquear.
            assertThatCode(() -> openAccountQueryPort.lockForUpdate(CUENTA, COMPANY))
                    .doesNotThrowAnyException();
            assertThatCode(() -> openAccountQueryPort.lockForUpdate(CUENTA_AJENA, COMPANY))
                    .doesNotThrowAnyException();
        }
    }
}
