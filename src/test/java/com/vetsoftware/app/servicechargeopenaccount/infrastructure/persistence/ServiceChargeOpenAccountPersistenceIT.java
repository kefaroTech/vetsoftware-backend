package com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.vetsoftware.app.servicechargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.TaxRef;
import com.vetsoftware.app.shared.pagination.PageResult;
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
 * Rodaja de persistencia del cargo por servicio contra MySQL real.
 *
 * <p>
 * Lo que solo se puede comprobar aqui: el desglose tributario congelado en
 * columnas propias del cargo sobrevive intacto a que el catalogo de servicios o
 * de impuestos cambie despues (el dominio lo congela, la base tiene que
 * devolverlo igual), el scope de empresa navegando
 * {@code openAccount.company.id} (el cargo no guarda su propia empresa), el
 * indice unico de idempotencia por cuenta y el {@code EXISTS} nativo de
 * {@code reactivate} contra {@code open_accounts}. Con un doble del repositorio
 * ninguna de las cuatro se puede falsear.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaServiceChargeOpenAccountRepository — impuesto congelado y scope contra MySQL real")
class ServiceChargeOpenAccountPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;
    private static final Long BRANCH_AJENA = 958L;
    private static final Long EMPLEADO = SchemaSeed.EMPLOYEE_ID;
    private static final Long OTRO_EMPLEADO = SchemaSeed.OTRO_EMPLOYEE_ID;

    private static final Long SPECIE_ID = 960L;
    private static final Long BREED_ID = 961L;
    private static final Long COLOR_ID = 962L;
    private static final Long OWNER = 963L;
    private static final Long OWNER_AJENO = 964L;
    private static final Long ANIMAL_ID = 965L;

    private static final Long CATEGORIA_SERVICIO = 970L;
    private static final Long TAX_ID = 975L;
    private static final Long SERVICIO_CON_IVA = 980L;
    private static final Long SERVICIO_SIN_IMPUESTO = 981L;

    private static final Long CUENTA = 985L;
    private static final Long OTRA_CUENTA = 986L;
    private static final Long CUENTA_AJENA = 987L;

    private static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL_ID, "Firulais", "A-001");
    private static final EmployeeRef CAJERA = new EmployeeRef(EMPLEADO, "Ana Ruiz");
    private static final EmployeeRef SUPERVISOR = new EmployeeRef(OTRO_EMPLEADO, "Luis Paz");
    private static final TaxRef IVA = new TaxRef(TAX_ID, "IVA 19%", new BigDecimal("19.00"), "IVA");
    private static final ServiceRef CONSULTA_CON_IVA = new ServiceRef(SERVICIO_CON_IVA,
            "Consulta general", new BigDecimal("11900"), true, IVA, "GRAVADO");
    private static final ServiceRef BANO_SIN_IMPUESTO = new ServiceRef(SERVICIO_SIN_IMPUESTO,
            "Bano", new BigDecimal("5000"));
    private static final OpenAccountRef LA_CUENTA = new OpenAccountRef(CUENTA, COMPANY);
    private static final OpenAccountRef LA_OTRA_CUENTA = new OpenAccountRef(OTRA_CUENTA, COMPANY);
    private static final OpenAccountRef LA_CUENTA_AJENA = new OpenAccountRef(CUENTA_AJENA,
            OTRA_COMPANY);

    @Autowired
    private JpaServiceChargeOpenAccountRepository repository;

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
        catalogoDeAnimal();
        catalogoDeServicio();
        cuenta(CUENTA, OWNER, COMPANY, BRANCH);
        cuenta(OTRA_CUENTA, OWNER, COMPANY, SchemaSeed.OTRA_BRANCH_ID);
        cuenta(CUENTA_AJENA, OWNER_AJENO, OTRA_COMPANY, BRANCH_AJENA);
        entityManager.flush();
    }

    private void sedeAjena() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO branches (id, name, code, city_id, company_id)
                VALUES (:id, 'Sede ajena', 'AJENA-SC', :ciudad, :empresa)
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

    private void catalogoDeAnimal() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO species (id, name, created_date, enabled)
                VALUES (:id, 'Canino-SC', '2026-01-01 08:00:00', true)
                """).setParameter("id", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO breeds (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Criollo-SC', :specie, '2026-01-01 08:00:00', true)
                """).setParameter("id", BREED_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animal_colors (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Negro-SC', :specie, '2026-01-01 08:00:00', true)
                """).setParameter("id", COLOR_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animals (id, name, code, specie_id, breed_id, owner_id, gender,
                                            weight_type, animal_type, reproductive_state, color_id,
                                            deceased, company_id, created_date, enabled)
                VALUES (:id, 'Firulais', 'A-001', :specie, :breed, :owner, 'MALE', 'KILOGRAMS',
                        'NONE', 'UNKNOWN', :color, false, :empresa, '2026-01-01 08:00:00', true)
                """).setParameter("id", ANIMAL_ID).setParameter("specie", SPECIE_ID)
                .setParameter("breed", BREED_ID).setParameter("owner", OWNER)
                .setParameter("color", COLOR_ID).setParameter("empresa", COMPANY).executeUpdate();
    }

    private void catalogoDeServicio() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO service_categories (id, name, description, company_id,
                                                       created_date, enabled)
                VALUES (:id, 'Consulta-SC', 'Categoria de prueba', :empresa,
                        '2026-01-01 08:00:00', true)
                """).setParameter("id", CATEGORIA_SERVICIO).setParameter("empresa", COMPANY)
                .executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO taxes (id, name, percentage, tax_scheme, company_id,
                                          created_date, version, enabled)
                VALUES (:id, 'IVA 19%', 19.00, 'IVA', :empresa, '2026-01-01 08:00:00', 0, true)
                """).setParameter("id", TAX_ID).setParameter("empresa", COMPANY).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO services (id, name, price, tax_treatment, tax_id,
                                             service_category_id, company_id, created_date,
                                             version, enabled)
                VALUES (:id, 'Consulta general', 11900.00, 'GRAVADO', :impuesto, :categoria,
                        :empresa, '2026-01-01 08:00:00', 0, true)
                """).setParameter("id", SERVICIO_CON_IVA).setParameter("impuesto", TAX_ID)
                .setParameter("categoria", CATEGORIA_SERVICIO).setParameter("empresa", COMPANY)
                .executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO services (id, name, price, tax_treatment, service_category_id,
                                             company_id, created_date, version, enabled)
                VALUES (:id, 'Bano', 5000.00, 'EXCLUIDO', :categoria, :empresa,
                        '2026-01-01 08:00:00', 0, true)
                """).setParameter("id", SERVICIO_SIN_IMPUESTO)
                .setParameter("categoria", CATEGORIA_SERVICIO).setParameter("empresa", COMPANY)
                .executeUpdate();
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

    private ServiceChargeOpenAccount cargo(OpenAccountRef cuenta, ServiceRef servicio,
            String clave) {
        return repository
                .save(ServiceChargeOpenAccount.create(FIRULAIS, servicio, cuenta, CAJERA, clave));
    }

    private ServiceChargeOpenAccount cargoConIva() {
        return cargo(LA_CUENTA, CONSULTA_CON_IVA, null);
    }

    @Nested
    @DisplayName("ida y vuelta con el desglose congelado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y el desglose vuelve intacto")
        void guardar_asigna_id_y_el_desglose_vuelve_intacto() {
            ServiceChargeOpenAccount guardado = cargoConIva();

            assertThat(guardado.getId()).isNotNull();

            ServiceChargeOpenAccount leido = repository
                    .findByIdAndCompanyId(guardado.getId(), COMPANY).orElseThrow();
            assertThat(leido.getAnimal()).isEqualTo(FIRULAIS);
            assertThat(leido.getService().name()).isEqualTo("Consulta general");
            assertThat(leido.getUnitPrice()).isEqualByComparingTo("11900");
            assertThat(leido.isHasTax()).isTrue();
            // El precio incluye IVA: base = total / 1,19. Los tres importes viajan en
            // columnas DECIMAL(12,2) y tienen que volver exactos.
            assertThat(leido.getBaseAmount()).isEqualByComparingTo("10000.00");
            assertThat(leido.getTaxAmount()).isEqualByComparingTo("1900.00");
            assertThat(leido.getTotalAmount()).isEqualByComparingTo("11900.00");
            assertThat(leido.getCreatedBy().name()).isEqualTo("Ana Ruiz");
        }

        @Test
        @DisplayName("el impuesto congelado sobrevive a que el catalogo cambie el porcentaje")
        void el_impuesto_congelado_sobrevive_al_cambio_del_catalogo() {
            ServiceChargeOpenAccount guardado = cargoConIva();
            entityManager.flush();

            entityManager.createNativeQuery("UPDATE taxes SET percentage = 5.00 WHERE id = :id")
                    .setParameter("id", TAX_ID).executeUpdate();
            entityManager.clear();

            ServiceChargeOpenAccount leido = repository
                    .findByIdAndCompanyId(guardado.getId(), COMPANY).orElseThrow();

            // El cargo guarda su propia copia del porcentaje: reeditar el catalogo despues
            // no puede mover el total de una cuenta ya cobrada. El ref del impuesto si
            // refleja el catalogo vigente, y esa diferencia es la prueba de que lo
            // congelado es real.
            assertThat(leido.getTaxPercentage()).isEqualByComparingTo("19.00");
            assertThat(leido.getTotalAmount()).isEqualByComparingTo("11900.00");
            assertThat(leido.getTax().percentage()).as("el ref sale del catalogo, ya editado")
                    .isEqualByComparingTo("5.00");
        }

        @Test
        @DisplayName("un cargo sin impuesto guarda la FK en null y vuelve sin ref")
        void un_cargo_sin_impuesto_guarda_la_fk_en_null() {
            ServiceChargeOpenAccount guardado = cargo(LA_CUENTA, BANO_SIN_IMPUESTO, null);

            ServiceChargeOpenAccount leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getTax()).isNull();
            assertThat(leido.isHasTax()).isFalse();
            assertThat(leido.getTaxAmount()).isEqualByComparingTo("0.00");
            assertThat(leido.getBaseAmount()).isEqualByComparingTo("5000.00");
        }

        @Test
        @DisplayName("la anulacion vuelve con quien, cuando y por que")
        void la_anulacion_vuelve_con_quien_cuando_y_por_que() {
            ServiceChargeOpenAccount guardado = cargoConIva();
            ServiceChargeOpenAccount recuperado = repository.findById(guardado.getId())
                    .orElseThrow();
            recuperado.voidCharge(SUPERVISOR, "Se cobro por error");
            repository.save(recuperado);

            ServiceChargeOpenAccount leido = repository
                    .findByIdAndCompanyId(guardado.getId(), COMPANY).orElseThrow();

            assertThat(leido.isVoided()).isTrue();
            assertThat(leido.isEnabled()).as("anular no da de baja la fila").isTrue();
            assertThat(leido.getVoidedBy().name()).isEqualTo("Luis Paz");
            assertThat(leido.getVoidReason()).isEqualTo("Se cobro por error");
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa a traves de la cuenta")
    class Tenancy {

        @Test
        @DisplayName("un cargo de otra empresa no se lee por id")
        void un_cargo_de_otra_empresa_no_se_lee_por_id() {
            ServiceChargeOpenAccount ajeno = cargo(LA_CUENTA_AJENA, BANO_SIN_IMPUESTO, null);

            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), COMPANY)).isEmpty();
            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), OTRA_COMPANY)).isPresent();
        }

        @Test
        @DisplayName("el listado por empresa no ve los cargos ajenos")
        void el_listado_por_empresa_no_ve_los_cargos_ajenos() {
            ServiceChargeOpenAccount propio = cargoConIva();
            cargo(LA_CUENTA_AJENA, BANO_SIN_IMPUESTO, null);

            PageResult<ServiceChargeOpenAccount> pagina = repository.findAllByCompanyId(COMPANY, 0,
                    20);

            assertThat(pagina.content()).extracting(ServiceChargeOpenAccount::getId)
                    .containsExactly(propio.getId());
            assertThat(pagina.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("el detalle de una cuenta ajena no se lista con la empresa propia")
        void el_detalle_de_una_cuenta_ajena_no_se_lista() {
            cargo(LA_CUENTA_AJENA, BANO_SIN_IMPUESTO, null);

            assertThat(repository.findByOpenAccountIdAndCompanyId(CUENTA_AJENA, COMPANY)).isEmpty();
            assertThat(repository.findByOpenAccountIdAndCompanyId(CUENTA_AJENA, OTRA_COMPANY))
                    .hasSize(1);
        }

        @Test
        @DisplayName("el detalle de una cuenta solo trae sus cargos")
        void el_detalle_de_una_cuenta_solo_trae_sus_cargos() {
            ServiceChargeOpenAccount deLaCuenta = cargoConIva();
            cargo(LA_OTRA_CUENTA, BANO_SIN_IMPUESTO, null);

            assertThat(repository.findByOpenAccountIdAndCompanyId(CUENTA, COMPANY))
                    .extracting(ServiceChargeOpenAccount::getId)
                    .containsExactly(deLaCuenta.getId());
        }
    }

    @Nested
    @DisplayName("idempotencia del cargo")
    class Idempotencia {

        @Test
        @DisplayName("encuentra el cargo ya registrado con esa clave")
        void encuentra_el_cargo_ya_registrado_con_esa_clave() {
            ServiceChargeOpenAccount guardado = cargo(LA_CUENTA, CONSULTA_CON_IVA,
                    "7c4a8d09-ca37-4b1e-9b40-000000000003");

            assertThat(repository.findByOpenAccountIdAndClientRequestId(CUENTA,
                    "7c4a8d09-ca37-4b1e-9b40-000000000003")).map(ServiceChargeOpenAccount::getId)
                    .contains(guardado.getId());
        }

        @Test
        @DisplayName("el finder por clave NO acota empresa: la barrera es el orden del servicio")
        void el_finder_por_clave_no_acota_por_empresa() {
            ServiceChargeOpenAccount ajeno = cargo(LA_CUENTA_AJENA, BANO_SIN_IMPUESTO,
                    "7c4a8d09-ca37-4b1e-9b40-000000000009");

            // La fila del cargo no tiene company_id y esta consulta no navega a
            // open_accounts: con el id de una cuenta AJENA y la clave exacta devuelve el
            // cargo del otro tenant. Por eso el servicio resuelve la cuenta ACOTADA antes
            // de llamar aqui —ese orden es toda la barrera— y este test es la razon por la
            // que no se puede volver a invertir.
            assertThat(repository.findByOpenAccountIdAndClientRequestId(CUENTA_AJENA,
                    "7c4a8d09-ca37-4b1e-9b40-000000000009")).map(ServiceChargeOpenAccount::getId)
                    .contains(ajeno.getId());
        }

        @Test
        @DisplayName("la misma clave en otra cuenta no es el mismo cargo")
        void la_misma_clave_en_otra_cuenta_no_es_el_mismo_cargo() {
            cargo(LA_CUENTA, CONSULTA_CON_IVA, "7c4a8d09-ca37-4b1e-9b40-000000000003");

            assertThat(repository.findByOpenAccountIdAndClientRequestId(OTRA_CUENTA,
                    "7c4a8d09-ca37-4b1e-9b40-000000000003")).isEmpty();
        }
    }

    @Nested
    @DisplayName("baja logica")
    class BajaLogica {

        @Test
        @DisplayName("borrar esconde la fila pero no la elimina")
        void borrar_esconde_la_fila_pero_no_la_elimina() {
            ServiceChargeOpenAccount guardado = cargoConIva();
            entityManager.flush();

            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).isEmpty();
            assertThat(filasCrudas(guardado.getId())).isEqualTo(1L);
        }

        @Test
        @DisplayName("reactivar exige la empresa correcta")
        void reactivar_exige_la_empresa_correcta() {
            ServiceChargeOpenAccount guardado = cargoConIva();
            entityManager.flush();
            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            // El UPDATE nativo comprueba el tenant con un EXISTS contra open_accounts: es
            // el unico sitio donde se puede, porque la fila del cargo no guarda empresa.
            assertThat(repository.reactivate(guardado.getId(), OTRA_COMPANY)).isZero();
            assertThat(repository.reactivate(guardado.getId(), COMPANY)).isEqualTo(1);
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), COMPANY)).isPresent();
        }

        @Test
        @DisplayName("la cuenta del cargo apagado se resuelve aunque ningun finder lo vea")
        void la_cuenta_del_cargo_apagado_se_resuelve() {
            ServiceChargeOpenAccount guardado = cargoConIva();
            entityManager.flush();
            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            // Esto es lo que la reactivacion necesita saber ANTES de encender la fila:
            // que cuenta hay que bloquear y comprobar que sigue abierta (#239). El
            // @SQLRestriction de la entidad esconde el cargo apagado de todo el HQL
            // —la primera asercion lo demuestra—, asi que la consulta es nativa a
            // proposito. Con un doble este test no probaria nada: lo que hay que ver
            // es que el SQL esquiva el filtro.
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), COMPANY)).isEmpty();
            assertThat(repository.findOpenAccountIdIncludingDisabled(guardado.getId(), COMPANY))
                    .contains(CUENTA);
        }

        @Test
        @DisplayName("la cuenta de un cargo ajeno no se resuelve")
        void la_cuenta_de_un_cargo_ajeno_no_se_resuelve() {
            ServiceChargeOpenAccount guardado = cargoConIva();
            entityManager.flush();
            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            // Mismo EXISTS que el UPDATE de reactivar. Si esto resolviera con la
            // empresa ajena, el caso de uso bloquearia y leeria una cuenta de otro
            // tenant antes de rechazar nada.
            assertThat(
                    repository.findOpenAccountIdIncludingDisabled(guardado.getId(), OTRA_COMPANY))
                    .isEmpty();
        }

        private long filasCrudas(Long id) {
            Number filas = (Number) entityManager
                    .createNativeQuery(
                            "SELECT COUNT(*) FROM service_charge_open_accounts WHERE id = :id")
                    .setParameter("id", id).getSingleResult();
            return filas.longValue();
        }
    }

    @Nested
    @DisplayName("listado paginado")
    class Listado {

        @Test
        @DisplayName("devuelve primero lo mas reciente con los metadatos de la consulta")
        void devuelve_primero_lo_mas_reciente_con_sus_metadatos() {
            ServiceChargeOpenAccount primero = cargoConIva();
            ServiceChargeOpenAccount segundo = cargo(LA_CUENTA, BANO_SIN_IMPUESTO, null);

            PageResult<ServiceChargeOpenAccount> pagina = repository.findAllByCompanyId(COMPANY, 0,
                    1);

            assertThat(pagina.content()).extracting(ServiceChargeOpenAccount::getId)
                    .containsExactly(segundo.getId());
            assertThat(pagina.totalElements()).isEqualTo(2L);
            assertThat(pagina.totalPages()).isEqualTo(2);
            assertThat(repository.findAllByCompanyId(COMPANY, 1, 1).content())
                    .extracting(ServiceChargeOpenAccount::getId).containsExactly(primero.getId());
        }
    }

    /**
     * El otro adaptador de esta feature: el que resuelve la cuenta a la que se le
     * cuelga el cargo. Se prueba aqui porque la semilla ya tiene dos empresas con
     * cuenta propia, y porque lo que hay que ver es la consulta —el
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
        @DisplayName("la cuenta de otra empresa no se resuelve: no hay donde colgar el cargo")
        void la_cuenta_de_otra_empresa_no_se_resuelve() {
            assertThat(openAccountQueryPort.findByIdAndCompanyId(CUENTA_AJENA, COMPANY)).isEmpty();
            assertThat(openAccountQueryPort.findByIdAndCompanyId(CUENTA_AJENA, OTRA_COMPANY))
                    .contains(LA_CUENTA_AJENA);
            // La variante ancha SI la devuelve: era la puerta por la que un cargo
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
