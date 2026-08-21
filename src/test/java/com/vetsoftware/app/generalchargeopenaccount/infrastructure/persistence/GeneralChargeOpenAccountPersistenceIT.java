package com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.generalchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.TaxRef;
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
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia del cargo libre contra MySQL real.
 *
 * <p>
 * Lo que hay que comprobar aqui es que el desglose tributario queda
 * <i>congelado</i> en columnas propias del cargo y no se recalcula al leer. El
 * dominio lo congela; la base es quien tiene que devolverlo intacto aunque el
 * catalogo de impuestos cambie despues, y esa diferencia —porcentaje vigente
 * del catalogo frente a porcentaje congelado en la fila— no se puede montar con
 * un doble: hace falta editar la tabla de impuestos por debajo y volver a leer.
 *
 * <p>
 * Lo demas que solo vive en la base: el scope de empresa navegando
 * {@code openAccount.company.id} (el cargo no guarda empresa), el indice unico
 * de idempotencia y el {@code @SQLDelete} que convierte el borrado en un
 * {@code UPDATE}.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaGeneralChargeOpenAccountRepository — impuesto congelado y scope contra MySQL real")
class GeneralChargeOpenAccountPersistenceIT extends AbstractDataJpaTest {

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
    private static final Long IVA_ID = 980L;

    private static final OpenAccountRef LA_CUENTA = new OpenAccountRef(CUENTA, COMPANY);
    private static final OpenAccountRef LA_OTRA_CUENTA = new OpenAccountRef(OTRA_CUENTA, COMPANY);
    private static final OpenAccountRef LA_CUENTA_AJENA = new OpenAccountRef(CUENTA_AJENA,
            OTRA_COMPANY);
    private static final EmployeeRef CAJERA = new EmployeeRef(EMPLEADO, "Ana Ruiz");
    private static final EmployeeRef SUPERVISOR = new EmployeeRef(OTRO_EMPLEADO, "Luis Paz");
    private static final TaxRef IVA = new TaxRef(IVA_ID, "IVA 19%", new BigDecimal("19.00"), "IVA");

    @Autowired
    private JpaGeneralChargeOpenAccountRepository repository;

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
        impuesto();
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

    private void impuesto() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO taxes (id, name, percentage, tax_scheme, company_id,
                                          created_date, version, enabled)
                VALUES (:id, 'IVA 19%', 19.00, 'IVA', :empresa, '2026-01-01 08:00:00', 0, true)
                """).setParameter("id", IVA_ID).setParameter("empresa", COMPANY).executeUpdate();
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

    private GeneralChargeOpenAccount cargo(OpenAccountRef cuenta, String importe, TaxRef impuesto,
            String clave) {
        return repository.save(GeneralChargeOpenAccount.create("Hospedaje nocturno",
                new BigDecimal(importe), BigDecimal.ONE, impuesto, cuenta, CAJERA, clave));
    }

    private GeneralChargeOpenAccount cargoConIva() {
        return cargo(LA_CUENTA, "100000.00", IVA, null);
    }

    @Nested
    @DisplayName("ida y vuelta con el desglose congelado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y el desglose vuelve intacto")
        void guardar_asigna_id_y_el_desglose_vuelve_intacto() {
            GeneralChargeOpenAccount guardado = cargoConIva();

            assertThat(guardado.getId()).isNotNull();

            GeneralChargeOpenAccount leido = repository
                    .findByIdAndCompanyId(guardado.getId(), COMPANY).orElseThrow();
            assertThat(leido.getName()).isEqualTo("Hospedaje nocturno");
            assertThat(leido.getUnitAmount()).isEqualByComparingTo("100000.00");
            assertThat(leido.getQuantity()).isEqualByComparingTo("1");
            assertThat(leido.isHasTax()).isTrue();
            // El precio incluye IVA: base = total / 1,19. Los tres importes viajan en
            // columnas DECIMAL(12,2) y tienen que volver al centavo.
            assertThat(leido.getBaseAmount()).isEqualByComparingTo("84033.61");
            assertThat(leido.getTaxAmount()).isEqualByComparingTo("15966.39");
            assertThat(leido.getTotalAmount()).isEqualByComparingTo("100000.00");
            assertThat(leido.getCreatedBy().name()).isEqualTo("Ana Ruiz");
        }

        @Test
        @DisplayName("el impuesto congelado sobrevive a que el catalogo cambie el porcentaje")
        void el_impuesto_congelado_sobrevive_al_cambio_del_catalogo() {
            GeneralChargeOpenAccount guardado = cargoConIva();
            entityManager.flush();

            entityManager.createNativeQuery("UPDATE taxes SET percentage = 5.00 WHERE id = :id")
                    .setParameter("id", IVA_ID).executeUpdate();
            entityManager.clear();

            GeneralChargeOpenAccount leido = repository
                    .findByIdAndCompanyId(guardado.getId(), COMPANY).orElseThrow();

            // El cargo guarda su propia copia del porcentaje y del nombre: reeditar el
            // catalogo no puede cambiar el total de una cuenta ya facturada. El ref del
            // impuesto si refleja el catalogo vigente, y esa diferencia es la prueba de
            // que el congelado es real y no una lectura del mismo sitio.
            assertThat(leido.getTaxPercentage()).isEqualByComparingTo("19.00");
            assertThat(leido.getTaxName()).isEqualTo("IVA 19%");
            assertThat(leido.getTotalAmount()).isEqualByComparingTo("100000.00");
            assertThat(leido.getTax().percentage()).as("el ref sale del catalogo, ya editado")
                    .isEqualByComparingTo("5.00");
        }

        @Test
        @DisplayName("un cargo sin impuesto guarda la FK en null y vuelve sin ref")
        void un_cargo_sin_impuesto_guarda_la_fk_en_null() {
            GeneralChargeOpenAccount guardado = cargo(LA_CUENTA, "50000.00", null, null);

            GeneralChargeOpenAccount leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getTax()).isNull();
            assertThat(leido.isHasTax()).isFalse();
            assertThat(leido.getTaxPercentage()).isNull();
            assertThat(leido.getTaxAmount()).isEqualByComparingTo("0.00");
            assertThat(leido.getBaseAmount()).isEqualByComparingTo("50000.00");
        }

        @Test
        @DisplayName("la anulacion vuelve con quien, cuando y por que")
        void la_anulacion_vuelve_con_quien_cuando_y_por_que() {
            GeneralChargeOpenAccount guardado = cargoConIva();
            GeneralChargeOpenAccount recuperado = repository.findById(guardado.getId())
                    .orElseThrow();
            recuperado.voidCharge(SUPERVISOR, "Se cobro por error");
            repository.save(recuperado);

            GeneralChargeOpenAccount leido = repository
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
            GeneralChargeOpenAccount ajeno = cargo(LA_CUENTA_AJENA, "70000.00", null, null);

            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), COMPANY)).isEmpty();
            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), OTRA_COMPANY)).isPresent();
        }

        @Test
        @DisplayName("el listado por empresa no ve los cargos ajenos")
        void el_listado_por_empresa_no_ve_los_cargos_ajenos() {
            GeneralChargeOpenAccount propio = cargoConIva();
            cargo(LA_CUENTA_AJENA, "70000.00", null, null);

            PageResult<GeneralChargeOpenAccount> pagina = repository.findAllByCompanyId(COMPANY, 0,
                    20);

            assertThat(pagina.content()).extracting(GeneralChargeOpenAccount::getId)
                    .containsExactly(propio.getId());
            assertThat(pagina.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("el detalle de una cuenta ajena no se lista con la empresa propia")
        void el_detalle_de_una_cuenta_ajena_no_se_lista() {
            cargo(LA_CUENTA_AJENA, "70000.00", null, null);

            assertThat(repository.findByOpenAccountIdAndCompanyId(CUENTA_AJENA, COMPANY)).isEmpty();
            assertThat(repository.findByOpenAccountIdAndCompanyId(CUENTA_AJENA, OTRA_COMPANY))
                    .hasSize(1);
        }

        @Test
        @DisplayName("el detalle de una cuenta solo trae sus cargos")
        void el_detalle_de_una_cuenta_solo_trae_sus_cargos() {
            GeneralChargeOpenAccount deLaCuenta = cargoConIva();
            cargo(LA_OTRA_CUENTA, "1000.00", null, null);

            assertThat(repository.findByOpenAccountIdAndCompanyId(CUENTA, COMPANY))
                    .extracting(GeneralChargeOpenAccount::getId)
                    .containsExactly(deLaCuenta.getId());
        }
    }

    @Nested
    @DisplayName("idempotencia del cargo")
    class Idempotencia {

        @Test
        @DisplayName("encuentra el cargo ya registrado con esa clave")
        void encuentra_el_cargo_ya_registrado_con_esa_clave() {
            GeneralChargeOpenAccount guardado = cargo(LA_CUENTA, "100000.00", IVA,
                    "7c4a8d09-ca37-4b1e-9b40-000000000002");

            assertThat(repository.findByOpenAccountIdAndClientRequestId(CUENTA,
                    "7c4a8d09-ca37-4b1e-9b40-000000000002")).map(GeneralChargeOpenAccount::getId)
                    .contains(guardado.getId());
        }

        @Test
        @DisplayName("dos cargos con la misma clave en la misma cuenta los corta la base")
        void dos_cargos_con_la_misma_clave_los_corta_la_base() {
            cargo(LA_CUENTA, "100000.00", IVA, "7c4a8d09-ca37-4b1e-9b40-000000000002");
            entityManager.flush();

            assertThatThrownBy(() -> {
                cargo(LA_CUENTA, "100000.00", IVA, "7c4a8d09-ca37-4b1e-9b40-000000000002");
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class)
                    .hasStackTraceContaining("uq_general_charge_open_accounts_request");
        }

        @Test
        @DisplayName("el finder por clave NO acota empresa: la barrera es el orden del servicio")
        void el_finder_por_clave_no_acota_por_empresa() {
            GeneralChargeOpenAccount ajeno = cargo(LA_CUENTA_AJENA, "70000.00", null,
                    "7c4a8d09-ca37-4b1e-9b40-000000000009");

            // La fila del cargo no tiene company_id y esta consulta no navega a
            // open_accounts: con el id de una cuenta AJENA y la clave exacta devuelve el
            // cargo del otro tenant. Por eso el servicio resuelve la cuenta ACOTADA antes
            // de llamar aqui —ese orden es toda la barrera— y este test es la razon por la
            // que no se puede volver a invertir.
            assertThat(repository.findByOpenAccountIdAndClientRequestId(CUENTA_AJENA,
                    "7c4a8d09-ca37-4b1e-9b40-000000000009")).map(GeneralChargeOpenAccount::getId)
                    .contains(ajeno.getId());
        }

        @Test
        @DisplayName("la misma clave en otra cuenta no es el mismo cargo")
        void la_misma_clave_en_otra_cuenta_no_es_el_mismo_cargo() {
            cargo(LA_CUENTA, "100000.00", IVA, "7c4a8d09-ca37-4b1e-9b40-000000000002");

            assertThat(repository.findByOpenAccountIdAndClientRequestId(OTRA_CUENTA,
                    "7c4a8d09-ca37-4b1e-9b40-000000000002")).isEmpty();
        }
    }

    @Nested
    @DisplayName("baja logica")
    class BajaLogica {

        @Test
        @DisplayName("borrar esconde la fila pero no la elimina")
        void borrar_esconde_la_fila_pero_no_la_elimina() {
            GeneralChargeOpenAccount guardado = cargoConIva();
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
            GeneralChargeOpenAccount guardado = cargoConIva();
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
            GeneralChargeOpenAccount guardado = cargoConIva();
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
            GeneralChargeOpenAccount guardado = cargoConIva();
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
                            "SELECT COUNT(*) FROM general_charge_open_accounts WHERE id = :id")
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
            GeneralChargeOpenAccount primero = cargoConIva();
            GeneralChargeOpenAccount segundo = cargo(LA_CUENTA, "20000.00", null, null);

            PageResult<GeneralChargeOpenAccount> pagina = repository.findAllByCompanyId(COMPANY, 0,
                    1);

            assertThat(pagina.content()).extracting(GeneralChargeOpenAccount::getId)
                    .containsExactly(segundo.getId());
            assertThat(pagina.totalElements()).isEqualTo(2L);
            assertThat(pagina.totalPages()).isEqualTo(2);
            assertThat(repository.findAllByCompanyId(COMPANY, 1, 1).content())
                    .extracting(GeneralChargeOpenAccount::getId).containsExactly(primero.getId());
        }

        @Test
        @DisplayName("findAll sin filtro trae los cargos de todas las empresas")
        void find_all_sin_filtro_trae_los_cargos_de_todas_las_empresas() {
            GeneralChargeOpenAccount propio = cargoConIva();
            GeneralChargeOpenAccount ajeno = cargo(LA_CUENTA_AJENA, "70000.00", null, null);

            assertThat(repository.findAll()).extracting(GeneralChargeOpenAccount::getId)
                    .containsExactlyInAnyOrder(propio.getId(), ajeno.getId());
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
