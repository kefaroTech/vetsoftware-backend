package com.vetsoftware.app.openaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountsSummaryDto;
import com.vetsoftware.app.openaccount.domain.BranchRef;
import com.vetsoftware.app.openaccount.domain.CompanyRef;
import com.vetsoftware.app.openaccount.domain.EmployeeRef;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.domain.OwnerRef;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia de la cuenta abierta contra MySQL real.
 *
 * <p>
 * Aqui casi nada de lo que se verifica existe en Java. La regla "una sola
 * cuenta abierta por propietario y sede" no la sostiene ningun {@code if} del
 * servicio sino dos columnas generadas ({@code active_open_owner_id},
 * {@code active_open_branch_id}) y un indice unico sobre ellas (migraciones 106
 * y 210): un doble del repositorio aceptaria felizmente las dos cuentas y la
 * carrera seguiria abierta en produccion. Lo mismo vale para la baja logica
 * —{@code @SQLDelete} + {@code @SQLRestriction} reescriben el SQL, no el
 * codigo—, para el {@code SELECT ... FOR UPDATE} acotado por empresa y para los
 * contadores del resumen, que son un {@code COUNT(CASE WHEN ...)} con un
 * {@code SUM} que devuelve {@code NULL} sobre cero filas.
 *
 * <p>
 * El aislamiento por empresa se prueba de verdad: hay dos empresas sembradas y
 * la ajena tiene su propia sede, su propio empleado y su propio propietario, de
 * modo que una fuga de tenant se ve como una fila de mas, no como un id que
 * coincide por casualidad.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaOpenAccountRepository — scope de empresa, unicidad y totales contra MySQL real")
class OpenAccountPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;
    private static final Long OTRA_BRANCH = SchemaSeed.OTRA_BRANCH_ID;
    private static final Long BRANCH_AJENA = 912L;
    private static final Long EMPLEADO_AJENO = 942L;
    private static final Long OWNER = 960L;
    private static final Long OTRO_OWNER = 961L;
    private static final Long OWNER_AJENO = 962L;

    private static final OwnerRef MARTA = new OwnerRef(OWNER, "Marta Diaz", "CC-1001");
    private static final OwnerRef JORGE = new OwnerRef(OTRO_OWNER, "Jorge Lima", "CC-1002");
    private static final OwnerRef AJENA = new OwnerRef(OWNER_AJENO, "Ana Ajena", "CC-2001");
    private static final CompanyRef EMPRESA = new CompanyRef(COMPANY, "Veterinaria de prueba",
            "900123456");
    private static final CompanyRef EMPRESA_AJENA = new CompanyRef(OTRA_COMPANY,
            "Veterinaria ajena", "900654321");
    private static final BranchRef SEDE = new BranchRef(BRANCH, "Sede Centro", "CENTRO");
    private static final BranchRef SEDE_NORTE = new BranchRef(OTRA_BRANCH, "Sede Norte", "NORTE");
    private static final BranchRef SEDE_AJENA = new BranchRef(BRANCH_AJENA, "Sede ajena", "AJENA");
    private static final EmployeeRef CAJERA = new EmployeeRef(SchemaSeed.EMPLOYEE_ID, "Ana Ruiz");
    private static final EmployeeRef CAJERA_AJENA = new EmployeeRef(EMPLEADO_AJENO, "Rosa Gil");

    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 1, 15, 9, 0);
    private static final LocalDateTime MAS_TARDE = LocalDateTime.of(2026, 1, 16, 9, 0);

    @Autowired
    private JpaOpenAccountRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        sede(BRANCH_AJENA, "Sede ajena", "AJENA", OTRA_COMPANY);
        empleado(EMPLEADO_AJENO, "EMP-901", "Rosa Gil", "rosa@ajena.local", OTRA_COMPANY);
        propietario(OWNER, "Marta Diaz", "CC-1001", COMPANY);
        propietario(OTRO_OWNER, "Jorge Lima", "CC-1002", COMPANY);
        propietario(OWNER_AJENO, "Ana Ajena", "CC-2001", OTRA_COMPANY);
        entityManager.flush();
    }

    private void sede(Long id, String nombre, String codigo, Long companyId) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO branches (id, name, code, city_id, company_id)
                VALUES (:id, :nombre, :codigo, :ciudad, :empresa)
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("codigo", codigo).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", companyId).executeUpdate();
    }

    private void empleado(Long id, String codigo, String nombre, String correo, Long companyId) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO employees (id, employee_code, hash_password, name, email,
                                              company_id)
                VALUES (:id, :codigo, 'x', :nombre, :correo, :empresa)
                """).setParameter("id", id).setParameter("codigo", codigo)
                .setParameter("nombre", nombre).setParameter("correo", correo)
                .setParameter("empresa", companyId).executeUpdate();
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

    private OpenAccount guardar(OwnerRef propietario, CompanyRef empresa, BranchRef sede,
            EmployeeRef creadaPor, OpenAccountStatus estado, String total, String pagado,
            LocalDateTime creada, boolean habilitada) {
        BigDecimal totalAmount = new BigDecimal(total);
        BigDecimal pagadoAmount = new BigDecimal(pagado);
        return repository.save(new OpenAccount(null, propietario, totalAmount, pagadoAmount,
                totalAmount.subtract(pagadoAmount), empresa, sede, estado, creadaPor, creada,
                habilitada, null, null, null, false, null, null));
    }

    private OpenAccount cuentaAbierta() {
        return guardar(MARTA, EMPRESA, SEDE, CAJERA, OpenAccountStatus.OPEN, "0.00", "0.00", CREADA,
                true);
    }

    private OpenAccount cuentaAjena() {
        return guardar(AJENA, EMPRESA_AJENA, SEDE_AJENA, CAJERA_AJENA, OpenAccountStatus.OPEN,
                "0.00", "0.00", CREADA, true);
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y version, y los companion refs vuelven resueltos")
        void guardar_asigna_id_y_los_refs_vuelven_resueltos() {
            OpenAccount guardada = cuentaAbierta();

            assertThat(guardada.getId()).isNotNull();
            assertThat(guardada.getVersion()).as("la columna @Version arranca al persistir")
                    .isNotNull();

            OpenAccount leida = repository.findByIdAndCompanyId(guardada.getId(), COMPANY)
                    .orElseThrow();
            // Estos nombres no estan en el agregado: los rehidrata el mapper desde los
            // @ManyToOne que trae el @EntityGraph. Con un doble saldrian los que el propio
            // test hubiera inventado.
            assertThat(leida.getOwner().name()).isEqualTo("Marta Diaz");
            assertThat(leida.getOwner().document()).isEqualTo("CC-1001");
            assertThat(leida.getCompany().identifier()).isEqualTo("900123456");
            assertThat(leida.getBranch().code()).isEqualTo("CENTRO");
            assertThat(leida.getCreatedBy().name()).isEqualTo("Ana Ruiz");
            assertThat(leida.getStatus()).isEqualTo(OpenAccountStatus.OPEN);
            assertThat(leida.getCreatedDate()).isEqualTo(CREADA);
            assertThat(leida.isEnabled()).isTrue();
            assertThat(leida.isReversed()).isFalse();
        }

        @Test
        @DisplayName("los importes conservan los dos decimales de DECIMAL(12,2)")
        void los_importes_conservan_los_dos_decimales() {
            // Es dinero: un redondeo del driver o una columna con menos escala se comen los
            // centavos del saldo, y el saldo es lo que se cobra en caja.
            OpenAccount guardada = guardar(MARTA, EMPRESA, SEDE, CAJERA, OpenAccountStatus.OPEN,
                    "1234.56", "34.56", CREADA, true);

            OpenAccount leida = repository.findById(guardada.getId()).orElseThrow();

            assertThat(leida.getTotalAmount()).isEqualByComparingTo("1234.56");
            assertThat(leida.getPaidAmount()).isEqualByComparingTo("34.56");
            assertThat(leida.getOutstandingAmount()).isEqualByComparingTo("1200.00");
        }

        @Test
        @DisplayName("el cierre vuelve con quien, cuando y por que")
        void el_cierre_vuelve_con_quien_cuando_y_por_que() {
            LocalDateTime cerrada = LocalDateTime.of(2026, 1, 20, 18, 30);
            OpenAccount guardada = repository
                    .save(new OpenAccount(null, MARTA, new BigDecimal("0"), new BigDecimal("0"),
                            new BigDecimal("0"), EMPRESA, SEDE, OpenAccountStatus.CANCEL, CAJERA,
                            CREADA, true, CAJERA, cerrada, "Incobrable", false, null, null));

            OpenAccount leida = repository.findByIdAndCompanyId(guardada.getId(), COMPANY)
                    .orElseThrow();

            assertThat(leida.getStatus()).isEqualTo(OpenAccountStatus.CANCEL);
            assertThat(leida.getClosedBy().id()).isEqualTo(SchemaSeed.EMPLOYEE_ID);
            assertThat(leida.getClosedAt()).isEqualTo(cerrada);
            assertThat(leida.getCloseReason()).isEqualTo("Incobrable");
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class Tenancy {

        @Test
        @DisplayName("una cuenta de otra empresa no se lee por id")
        void una_cuenta_de_otra_empresa_no_se_lee_por_id() {
            OpenAccount ajena = cuentaAjena();

            // findById a secas si la ve: por eso el puerto expone la variante scoped y es
            // la que usan los servicios. Adivinar un id no puede dar acceso a la cartera
            // de otro tenant.
            assertThat(repository.findByIdAndCompanyId(ajena.getId(), COMPANY)).isEmpty();
            assertThat(repository.findByIdAndCompanyId(ajena.getId(), OTRA_COMPANY)).isPresent();
        }

        @Test
        @DisplayName("el listado de la empresa no ve las cuentas ajenas")
        void el_listado_de_la_empresa_no_ve_las_cuentas_ajenas() {
            OpenAccount propia = cuentaAbierta();
            cuentaAjena();

            PageResult<OpenAccount> pagina = repository.findAllByCompanyId(COMPANY, null, 0, 20);

            assertThat(pagina.content()).extracting(OpenAccount::getId)
                    .containsExactly(propia.getId());
            assertThat(pagina.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("la busqueda no ve las cuentas ajenas")
        void la_busqueda_no_ve_las_cuentas_ajenas() {
            OpenAccount propia = cuentaAbierta();
            cuentaAjena();

            assertThat(repository.search(new SearchOpenAccountsCommand(COMPANY, null, null,
                    List.of(), null, 0, 20, null)).content()).extracting(OpenAccount::getId)
                    .containsExactly(propia.getId());
        }

        @Test
        @DisplayName("el resumen no suma las cuentas ajenas")
        void el_resumen_no_suma_las_cuentas_ajenas() {
            guardar(AJENA, EMPRESA_AJENA, SEDE_AJENA, CAJERA_AJENA, OpenAccountStatus.OPEN,
                    "90000.00", "0.00", CREADA, true);

            OpenAccountsSummaryDto resumen = repository.summarize(COMPANY, null);

            assertThat(resumen.openCount()).isZero();
            assertThat(resumen.totalOutstanding()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("el bloqueo pesimista scoped no alcanza una cuenta ajena")
        void el_bloqueo_pesimista_scoped_no_alcanza_una_cuenta_ajena() {
            OpenAccount ajena = cuentaAjena();

            // El FOR UPDATE de otro tenant no solo leeria datos ajenos: dejaria la fila
            // bloqueada hasta el commit, que es una denegacion de servicio barata.
            assertThat(repository.findByIdForUpdateAndCompanyId(ajena.getId(), COMPANY)).isEmpty();
            assertThat(repository.findByIdForUpdateAndCompanyId(ajena.getId(), OTRA_COMPANY))
                    .isPresent();
        }

        @Test
        @DisplayName("el bloqueo pesimista carga la cuenta con sus refs pese a no llevar fetch")
        void el_bloqueo_pesimista_carga_la_cuenta_con_sus_refs() {
            OpenAccount propia = cuentaAbierta();

            // La consulta con FOR UPDATE va deliberadamente sin @EntityGraph (no combina
            // con el join fetch). El mapper toca owner/company/branch/createdBy igualmente,
            // asi que si el lazy no se resolviera dentro de la transaccion esto reventaria.
            OpenAccount bloqueada = repository.findByIdForUpdate(propia.getId()).orElseThrow();

            assertThat(bloqueada.getOwner().name()).isEqualTo("Marta Diaz");
            assertThat(bloqueada.getBranch().code()).isEqualTo("CENTRO");
        }
    }

    @Nested
    @DisplayName("una sola cuenta abierta por propietario y sede")
    class Unicidad {

        @Test
        @DisplayName("dos cuentas abiertas del mismo propietario en la misma sede las corta la base")
        void dos_cuentas_abiertas_del_mismo_propietario_en_la_misma_sede_las_corta_la_base() {
            cuentaAbierta();

            // La garantia dura no esta en Java: es un indice unico sobre dos columnas
            // generadas (migraciones 106 y 210). El chequeo del servicio deja una ventana
            // de carrera; esto es lo que la cierra, y solo se ve contra MySQL.
            assertThatThrownBy(() -> {
                cuentaAbierta();
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class)
                    .hasStackTraceContaining("uq_open_accounts_active_owner_branch");
        }

        @Test
        @DisplayName("el mismo propietario si puede tener una cuenta abierta en otra sede")
        void el_mismo_propietario_si_puede_tener_una_cuenta_en_otra_sede() {
            cuentaAbierta();

            OpenAccount enNorte = guardar(MARTA, EMPRESA, SEDE_NORTE, CAJERA,
                    OpenAccountStatus.OPEN, "0.00", "0.00", CREADA, true);
            entityManager.flush();

            assertThat(enNorte.getId()).isNotNull();
            assertThat(repository.findAllByCompanyId(COMPANY, null, 0, 20).totalElements())
                    .isEqualTo(2L);
        }

        @Test
        @DisplayName("una cuenta cerrada deja abrir otra al mismo propietario en la misma sede")
        void una_cuenta_cerrada_deja_abrir_otra_al_mismo_propietario() {
            guardar(MARTA, EMPRESA, SEDE, CAJERA, OpenAccountStatus.CLOSE, "0.00", "0.00", CREADA,
                    true);

            // La columna generada vale NULL salvo que la cuenta este OPEN y habilitada, y
            // MySQL admite tantos NULL como quiera en un indice unico. Sin ese detalle, un
            // cliente no podria volver nunca a abrir cuenta.
            OpenAccount nueva = cuentaAbierta();
            entityManager.flush();

            assertThat(nueva.getId()).isNotNull();
        }

        @Test
        @DisplayName("existsActive reconoce la cuenta abierta y no la cerrada ni la de otra sede")
        void exists_active_reconoce_solo_la_cuenta_abierta_de_esa_sede() {
            cuentaAbierta();
            guardar(JORGE, EMPRESA, SEDE, CAJERA, OpenAccountStatus.CLOSE, "0.00", "0.00", CREADA,
                    true);

            assertThat(repository.existsActiveByOwnerIdAndBranchId(OWNER, BRANCH)).isTrue();
            assertThat(repository.existsActiveByOwnerIdAndBranchId(OWNER, OTRA_BRANCH)).isFalse();
            assertThat(repository.existsActiveByOwnerIdAndBranchId(OTRO_OWNER, BRANCH)).isFalse();
        }
    }

    @Nested
    @DisplayName("resumen de la pantalla")
    class Resumen {

        @Test
        @DisplayName("solo suma el saldo de las cuentas abiertas, no el de las canceladas")
        void solo_suma_el_saldo_de_las_cuentas_abiertas() {
            guardar(MARTA, EMPRESA, SEDE, CAJERA, OpenAccountStatus.OPEN, "50000.00", "10000.00",
                    CREADA, true);
            guardar(JORGE, EMPRESA, SEDE, CAJERA, OpenAccountStatus.CANCEL, "30000.00", "0.00",
                    CREADA, true);

            OpenAccountsSummaryDto resumen = repository.summarize(COMPANY, null);

            // Salvaguarda contable escrita en el dominio: el saldo de una cancelada es una
            // perdida dada de baja, no una cuenta por cobrar. Si el CASE WHEN del SUM se
            // cayera, el tablero mostraria 70.000 cobrables donde solo hay 40.000.
            assertThat(resumen.openCount()).isEqualTo(1L);
            assertThat(resumen.closedCount()).isEqualTo(1L);
            assertThat(resumen.totalOutstanding()).isEqualByComparingTo("40000.00");
        }

        @Test
        @DisplayName("sin cuentas abiertas el saldo es cero y no null")
        void sin_cuentas_abiertas_el_saldo_es_cero_y_no_null() {
            guardar(MARTA, EMPRESA, SEDE, CAJERA, OpenAccountStatus.CLOSE, "0.00", "0.00", CREADA,
                    true);

            // SUM sobre cero filas devuelve NULL en SQL, no cero: el adaptador lo
            // normaliza. Sin eso el front recibe null y pinta "NaN" en el tablero.
            assertThat(repository.summarize(COMPANY, null).totalOutstanding())
                    .isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("el resumen se puede acotar a una sede")
        void el_resumen_se_puede_acotar_a_una_sede() {
            guardar(MARTA, EMPRESA, SEDE, CAJERA, OpenAccountStatus.OPEN, "50000.00", "0.00",
                    CREADA, true);
            guardar(JORGE, EMPRESA, SEDE_NORTE, CAJERA, OpenAccountStatus.OPEN, "20000.00", "0.00",
                    CREADA, true);

            assertThat(repository.summarize(COMPANY, BRANCH).totalOutstanding())
                    .isEqualByComparingTo("50000.00");
            assertThat(repository.summarize(COMPANY, null).totalOutstanding())
                    .isEqualByComparingTo("70000.00");
        }
    }

    @Nested
    @DisplayName("listado y busqueda")
    class Listados {

        @Test
        @DisplayName("el listado pagina y devuelve los metadatos de la consulta, no de la pagina")
        void el_listado_pagina_y_devuelve_los_metadatos() {
            cuentaAbierta();
            guardar(JORGE, EMPRESA, SEDE, CAJERA, OpenAccountStatus.OPEN, "0.00", "0.00", CREADA,
                    true);

            PageResult<OpenAccount> pagina = repository.findAllByCompanyId(COMPANY, null, 0, 1);

            assertThat(pagina.content()).hasSize(1);
            assertThat(pagina.totalElements()).isEqualTo(2L);
            assertThat(pagina.totalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("el listado devuelve primero lo mas reciente")
        void el_listado_devuelve_primero_lo_mas_reciente() {
            OpenAccount primera = cuentaAbierta();
            OpenAccount segunda = guardar(JORGE, EMPRESA, SEDE, CAJERA, OpenAccountStatus.OPEN,
                    "0.00", "0.00", CREADA, true);

            assertThat(repository.findAllByCompanyId(COMPANY, null, 0, 20).content())
                    .extracting(OpenAccount::getId)
                    .containsExactly(segunda.getId(), primera.getId());
        }

        @Test
        @DisplayName("el filtro de sede es opcional: null trae todas")
        void el_filtro_de_sede_es_opcional() {
            OpenAccount enCentro = cuentaAbierta();
            guardar(JORGE, EMPRESA, SEDE_NORTE, CAJERA, OpenAccountStatus.OPEN, "0.00", "0.00",
                    CREADA, true);

            assertThat(repository.findAllByCompanyId(COMPANY, BRANCH, 0, 20).content())
                    .extracting(OpenAccount::getId).containsExactly(enCentro.getId());
            assertThat(repository.findAllByCompanyId(COMPANY, null, 0, 20).content()).hasSize(2);
        }

        @Test
        @DisplayName("la busqueda por estados es un IN: cobradas y canceladas a la vez")
        void la_busqueda_por_estados_es_un_in() {
            cuentaAbierta();
            OpenAccount cancelada = guardar(JORGE, EMPRESA, SEDE, CAJERA, OpenAccountStatus.CANCEL,
                    "0.00", "0.00", CREADA, true);

            PageResult<OpenAccount> cerradas = repository.search(new SearchOpenAccountsCommand(
                    COMPANY, null, null, List.of(OpenAccountStatus.CLOSE, OpenAccountStatus.CANCEL),
                    null, 0, 20, null));

            assertThat(cerradas.content()).extracting(OpenAccount::getId)
                    .containsExactly(cancelada.getId());
        }

        @Test
        @DisplayName("el buscador encuentra por nombre o documento sin distinguir mayusculas")
        void el_buscador_encuentra_por_nombre_o_documento() {
            OpenAccount deMarta = cuentaAbierta();
            guardar(JORGE, EMPRESA, SEDE, CAJERA, OpenAccountStatus.OPEN, "0.00", "0.00", CREADA,
                    true);

            // El LIKE en minusculas se resuelve en la base sobre las columnas del
            // propietario; el agregado ni siquiera tiene el texto que se busca.
            assertThat(buscar("MARTA").content()).extracting(OpenAccount::getId)
                    .containsExactly(deMarta.getId());
            assertThat(buscar("cc-1001").content()).extracting(OpenAccount::getId)
                    .containsExactly(deMarta.getId());
            assertThat(buscar("  marta  ").content()).as("el termino se recorta antes del LIKE")
                    .hasSize(1);
        }

        @Test
        @DisplayName("el buscador con un termino en blanco no filtra nada")
        void el_buscador_con_un_termino_en_blanco_no_filtra() {
            cuentaAbierta();

            assertThat(buscar("   ").content()).hasSize(1);
        }

        @Test
        @DisplayName("una cuenta deshabilitada no aparece ni pidiendo enabled=false")
        void una_cuenta_deshabilitada_no_aparece_ni_pidiendo_enabled_false() {
            guardar(MARTA, EMPRESA, SEDE, CAJERA, OpenAccountStatus.OPEN, "0.00", "0.00", CREADA,
                    false);

            // Hallazgo de esta rodaja: el @SQLRestriction("enabled = true") de la entidad
            // se anade a TODA consulta JPQL/criteria, asi que el filtro `enabled` del
            // buscador nunca puede devolver las deshabilitadas — pedir enabled=false da
            // siempre vacio. La papelera solo es alcanzable por el UPDATE nativo de
            // reactivate(). Se deja fijado el comportamiento real, no el aparente.
            assertThat(repository.search(new SearchOpenAccountsCommand(COMPANY, null, false,
                    List.of(), null, 0, 20, null)).content()).isEmpty();
            assertThat(repository.search(new SearchOpenAccountsCommand(COMPANY, null, null,
                    List.of(), null, 0, 20, null)).content()).isEmpty();
        }

        @Test
        @DisplayName("la busqueda ordena por fecha de creacion descendente")
        void la_busqueda_ordena_por_fecha_de_creacion_descendente() {
            OpenAccount vieja = guardar(MARTA, EMPRESA, SEDE, CAJERA, OpenAccountStatus.OPEN,
                    "0.00", "0.00", CREADA, true);
            OpenAccount nueva = guardar(JORGE, EMPRESA, SEDE, CAJERA, OpenAccountStatus.OPEN,
                    "0.00", "0.00", MAS_TARDE, true);

            assertThat(buscar(null).content()).extracting(OpenAccount::getId)
                    .containsExactly(nueva.getId(), vieja.getId());
        }

        private PageResult<OpenAccount> buscar(String termino) {
            return repository.search(new SearchOpenAccountsCommand(COMPANY, null, null, List.of(),
                    termino, 0, 20, null));
        }

        @Test
        @DisplayName("la busqueda filtra por ownerId cuando se especifica")
        void la_busqueda_filtra_por_owner_id_cuando_se_especifica() {
            OpenAccount deMarta = cuentaAbierta();
            guardar(JORGE, EMPRESA, SEDE, CAJERA, OpenAccountStatus.OPEN, "0.00", "0.00", CREADA,
                    true);

            PageResult<OpenAccount> resultado = repository.search(new SearchOpenAccountsCommand(
                    COMPANY, OWNER, null, List.of(), null, 0, 20, null));

            assertThat(resultado.content()).extracting(OpenAccount::getId)
                    .containsExactly(deMarta.getId());
        }

        @Test
        @DisplayName("la busqueda filtra por sede cuando se especifica")
        void la_busqueda_filtra_por_sede_cuando_se_especifica() {
            OpenAccount enCentro = cuentaAbierta();
            guardar(JORGE, EMPRESA, SEDE_NORTE, CAJERA, OpenAccountStatus.OPEN, "0.00", "0.00",
                    CREADA, true);

            PageResult<OpenAccount> resultado = repository.search(new SearchOpenAccountsCommand(
                    COMPANY, null, null, List.of(), null, 0, 20, BRANCH));

            assertThat(resultado.content()).extracting(OpenAccount::getId)
                    .containsExactly(enCentro.getId());
        }
    }

    @Nested
    @DisplayName("baja logica")
    class BajaLogica {

        @Test
        @DisplayName("borrar no borra la fila: la esconde")
        void borrar_no_borra_la_fila_la_esconde() {
            OpenAccount cuenta = cuentaAbierta();
            entityManager.flush();

            repository.delete(cuenta.getId());
            entityManager.flush();
            entityManager.clear();

            // El @SQLDelete convierte el DELETE en UPDATE enabled = false. La fila tiene
            // que seguir ahi —hay cargos y abonos colgando de ella— y a la vez dejar de
            // verse. Solo la base puede decir si las dos cosas pasan.
            assertThat(repository.findById(cuenta.getId())).isEmpty();
            assertThat(filasCrudas(cuenta.getId())).isEqualTo(1L);
        }

        @Test
        @DisplayName("reactivar la vuelve a hacer visible")
        void reactivar_la_vuelve_a_hacer_visible() {
            OpenAccount cuenta = cuentaAbierta();
            entityManager.flush();
            repository.delete(cuenta.getId());
            entityManager.flush();
            entityManager.clear();

            int reactivadas = repository.reactivate(cuenta.getId(), COMPANY);

            assertThat(reactivadas).isEqualTo(1);
            assertThat(repository.findByIdAndCompanyId(cuenta.getId(), COMPANY)).isPresent();
        }

        @Test
        @DisplayName("reactivar una cuenta inexistente no toca ninguna fila")
        void reactivar_una_cuenta_inexistente_no_toca_ninguna_fila() {
            assertThat(repository.reactivate(-1L, COMPANY)).isZero();
        }

        @Test
        @DisplayName("reactivar con el companyId de OTRA empresa afecta 0 filas y la deja oculta")
        void reactivar_con_la_empresa_ajena_no_toca_ninguna_fila() {
            // Antes el service reactivaba primero y comparaba la empresa despues: solo el
            // rollback deshacia la escritura ajena. Ahora la empresa esta en el WHERE, asi
            // que no hay escritura que deshacer.
            OpenAccount cuenta = cuentaAbierta();
            entityManager.flush();
            repository.delete(cuenta.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.reactivate(cuenta.getId(), OTRA_COMPANY)).isZero();
            entityManager.clear();

            assertThat(repository.findById(cuenta.getId())).isEmpty();
        }

        private long filasCrudas(Long id) {
            Number filas = (Number) entityManager
                    .createNativeQuery("SELECT COUNT(*) FROM open_accounts WHERE id = :id")
                    .setParameter("id", id).getSingleResult();
            return filas.longValue();
        }
    }
}
