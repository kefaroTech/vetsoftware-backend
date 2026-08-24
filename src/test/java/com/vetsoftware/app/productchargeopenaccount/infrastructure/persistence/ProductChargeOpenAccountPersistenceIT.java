package com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.productchargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.productchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.productchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductRef;
import com.vetsoftware.app.productchargeopenaccount.domain.TaxRef;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia del cargo por producto contra MySQL real.
 *
 * <p>
 * Lo que <b>solo</b> se puede comprobar aqui, y por eso este fichero existe:
 *
 * <ul>
 * <li><b>Las seis referencias perezosas de {@code save(...)}.</b> El adaptador
 * resuelve animal, producto, impuesto, cuenta, autor y anulador con
 * {@code getReferenceById}, que devuelve un proxy sin tocar la base: un id
 * inexistente NO falla ahi, falla al hacer flush contra la FK. Un doble del
 * repositorio devuelve verde en ese mismo escenario.</li>
 * <li><b>El scope de empresa a dos tablas.</b> El cargo no guarda
 * {@code company_id}: {@code findByIdAndCompanyId} navega
 * {@code open_account.company.id}. Ese salto es SQL, no logica Java.</li>
 * <li><b>La clave unica de idempotencia.</b>
 * {@code uq_product_charge_open_accounts_request} es
 * {@code (open_account_id, client_request_id)} y NO incluye {@code enabled}: la
 * cara "sigue siendo conflicto" y la cara "la fila dada de baja sigue ocupando
 * la clave" solo las dice el motor.</li>
 * <li><b>La baja logica.</b> {@code @SQLDelete} + {@code @SQLRestriction} son
 * SQL generado por Hibernate; sin base real no se ejecutan nunca.</li>
 * </ul>
 *
 * <p>
 * <b>Sin {@code INSERT IGNORE} en la semilla propia.</b> Silencia tambien
 * errores de FK, de {@code CHECK} y de columna {@code NOT NULL} omitida: el
 * seed "funciona", no inserta nada, y el fallo aparece cuarenta lineas despues
 * como un {@code EntityNotFound} incomprensible. Se usa
 * {@code ON DUPLICATE KEY UPDATE id = id}, que solo perdona la colision de
 * clave.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaProductChargeOpenAccountRepository — referencias, scope e idempotencia contra MySQL real")
class ProductChargeOpenAccountPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;

    private static final Long BRANCH_AJENA = 1958L;
    private static final Long SPECIE_ID = 1960L;
    private static final Long BREED_ID = 1961L;
    private static final Long COLOR_ID = 1962L;
    private static final Long OWNER = 1963L;
    private static final Long OWNER_AJENO = 1964L;
    private static final Long ANIMAL_ID = 1965L;
    private static final Long TAX_ID = 1975L;

    private static final Long CUENTA = 1985L;
    private static final Long OTRA_CUENTA = 1986L;
    private static final Long CUENTA_AJENA = 1987L;

    /** Id que no existe en ninguna tabla: el proxy perezoso lo acepta igual. */
    private static final Long ID_FANTASMA = 8_888_888L;

    private static final String CLAVE = "7c4a8d09-ca37-4b1e-9b40-000000000042";

    private static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL_ID, "Firulais", "A-PC-001");
    private static final EmployeeRef CAJERA = new EmployeeRef(SchemaSeed.EMPLOYEE_ID, "Ana Ruiz");
    private static final EmployeeRef SUPERVISOR = new EmployeeRef(SchemaSeed.OTRO_EMPLOYEE_ID,
            "Luis Paz");

    private static final TaxRef IVA = new TaxRef(TAX_ID, "IVA 19%", new BigDecimal("19.00"), "IVA");
    private static final TaxRef IVA_FANTASMA = new TaxRef(ID_FANTASMA, "IVA inexistente",
            new BigDecimal("19.00"), "IVA");

    /**
     * El precio del ref (11.900 con IVA) es deliberadamente distinto del que tiene
     * la fila de {@code products} en la semilla (10.000): asi la asercion distingue
     * el precio congelado en el cargo del precio vigente del catalogo.
     */
    private static final BigDecimal PRECIO_CONGELADO = new BigDecimal("11900");

    private static final ProductRef ALIMENTO_CON_IVA = new ProductRef(SchemaSeed.PRODUCT_ID,
            "Amoxicilina 500mg", "SKU-100", PRECIO_CONGELADO, true, IVA, "GRAVADO");
    private static final ProductRef COLLAR_SIN_IMPUESTO = new ProductRef(SchemaSeed.OTRO_PRODUCT_ID,
            "Ivermectina 1%", "SKU-200", new BigDecimal("5000"));
    private static final ProductRef ALIMENTO_CON_IVA_FANTASMA = new ProductRef(
            SchemaSeed.PRODUCT_ID, "Amoxicilina 500mg", "SKU-100", PRECIO_CONGELADO, true,
            IVA_FANTASMA, "GRAVADO");

    private static final OpenAccountRef LA_CUENTA = new OpenAccountRef(CUENTA, COMPANY);
    private static final OpenAccountRef LA_OTRA_CUENTA = new OpenAccountRef(OTRA_CUENTA, COMPANY);
    private static final OpenAccountRef LA_CUENTA_AJENA = new OpenAccountRef(CUENTA_AJENA,
            OTRA_COMPANY);

    @Autowired
    private JpaProductChargeOpenAccountRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        sedeAjena();
        propietario(OWNER, "Marta Diaz", "CC-PC-1001", COMPANY);
        propietario(OWNER_AJENO, "Ana Ajena", "CC-PC-2001", OTRA_COMPANY);
        catalogoDeAnimal();
        impuesto();
        cuenta(CUENTA, OWNER, COMPANY, BRANCH);
        cuenta(OTRA_CUENTA, OWNER, COMPANY, SchemaSeed.OTRA_BRANCH_ID);
        cuenta(CUENTA_AJENA, OWNER_AJENO, OTRA_COMPANY, BRANCH_AJENA);
        entityManager.flush();
    }

    private void sedeAjena() {
        entityManager.createNativeQuery("""
                INSERT INTO branches (id, name, code, city_id, company_id)
                VALUES (:id, 'Sede ajena PC', 'AJENA-PC', :ciudad, :empresa)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", BRANCH_AJENA).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", OTRA_COMPANY).executeUpdate();
    }

    private void propietario(Long id, String nombre, String documento, Long companyId) {
        entityManager.createNativeQuery("""
                INSERT INTO owners (id, name, document, document_type, person_type,
                                    withholding_agent, tax_regime, fiscal_responsibility,
                                    city_id, company_id, created_date, enabled)
                VALUES (:id, :nombre, :documento, 'CEDULA_CIUDADANIA', 'NATURAL', false,
                        'NO_RESPONSABLE_IVA', 'NO_APLICA', :ciudad, :empresa,
                        '2026-01-01 08:00:00', true)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("documento", documento).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", companyId).executeUpdate();
    }

    private void catalogoDeAnimal() {
        entityManager.createNativeQuery("""
                INSERT INTO species (id, name, created_date, enabled)
                VALUES (:id, 'Canino-PC', '2026-01-01 08:00:00', true)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO breeds (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Criollo-PC', :specie, '2026-01-01 08:00:00', true)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", BREED_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO animal_colors (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Negro-PC', :specie, '2026-01-01 08:00:00', true)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", COLOR_ID).setParameter("specie", SPECIE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO animals (id, name, code, specie_id, breed_id, owner_id, gender,
                                     weight_type, animal_type, reproductive_state, color_id,
                                     deceased, company_id, created_date, enabled)
                VALUES (:id, 'Firulais', 'A-PC-001', :specie, :breed, :owner, 'MALE', 'KILOGRAMS',
                        'NONE', 'UNKNOWN', :color, false, :empresa, '2026-01-01 08:00:00', true)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", ANIMAL_ID).setParameter("specie", SPECIE_ID)
                .setParameter("breed", BREED_ID).setParameter("owner", OWNER)
                .setParameter("color", COLOR_ID).setParameter("empresa", COMPANY).executeUpdate();
    }

    private void impuesto() {
        entityManager.createNativeQuery("""
                INSERT INTO taxes (id, name, percentage, tax_scheme, company_id,
                                   created_date, version, enabled)
                VALUES (:id, 'IVA 19%', 19.00, 'IVA', :empresa, '2026-01-01 08:00:00', 0, true)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", TAX_ID).setParameter("empresa", COMPANY).executeUpdate();
    }

    private void cuenta(Long id, Long ownerId, Long companyId, Long branchId) {
        entityManager.createNativeQuery("""
                INSERT INTO open_accounts (id, total_amount, paid_amount, outstanding_amount,
                                           owner_id, company_id, branch_id, created_by_id,
                                           created_date, enabled, status, reversed, version)
                VALUES (:id, 0.00, 0.00, 0.00, :owner, :empresa, :sede, :empleado,
                        '2026-01-15 09:00:00', true, 'OPEN', false, 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("owner", ownerId)
                .setParameter("empresa", companyId).setParameter("sede", branchId)
                .setParameter("empleado", SchemaSeed.EMPLOYEE_ID).executeUpdate();
    }

    private ProductChargeOpenAccount cargo(OpenAccountRef cuenta, ProductRef producto, int cantidad,
            String clave) {
        return repository.save(ProductChargeOpenAccount.create(FIRULAIS, producto, cantidad, cuenta,
                CAJERA, clave));
    }

    /** Dos unidades de 11.900 con IVA 19 %: 23.800 = 20.000 de base + 3.800. */
    private ProductChargeOpenAccount dosUnidadesConIva() {
        return cargo(LA_CUENTA, ALIMENTO_CON_IVA, 2, null);
    }

    private long filasCrudas(Long id) {
        Number filas = (Number) entityManager
                .createNativeQuery(
                        "SELECT COUNT(*) FROM product_charge_open_accounts WHERE id = :id")
                .setParameter("id", id).getSingleResult();
        return filas.longValue();
    }

    @Nested
    @DisplayName("ida y vuelta con el desglose congelado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y el desglose tributario vuelve intacto")
        void guardar_asigna_id_y_el_desglose_vuelve_intacto() {
            ProductChargeOpenAccount guardado = dosUnidadesConIva();

            assertThat(guardado.getId()).isNotNull();

            entityManager.flush();
            entityManager.clear();
            ProductChargeOpenAccount leido = repository
                    .findByIdAndCompanyId(guardado.getId(), COMPANY).orElseThrow();

            assertThat(leido.getAnimal()).isEqualTo(FIRULAIS);
            assertThat(leido.getProduct().code()).isEqualTo("SKU-100");
            assertThat(leido.getQuantity()).isEqualTo(2);
            assertThat(leido.getUnitPrice()).isEqualByComparingTo("11900");
            assertThat(leido.isHasTax()).isTrue();
            assertThat(leido.getTaxPercentage()).isEqualByComparingTo("19.00");
            assertThat(leido.getTaxName()).isEqualTo("IVA 19%");
            assertThat(leido.getTaxScheme()).isEqualTo("IVA");
            assertThat(leido.getTaxTreatment()).isEqualTo("GRAVADO");
            // Los tres importes viajan en DECIMAL(12,2) y tienen que volver exactos.
            assertThat(leido.getBaseAmount()).isEqualByComparingTo("20000.00");
            assertThat(leido.getTaxAmount()).isEqualByComparingTo("3800.00");
            assertThat(leido.getTotalAmount()).isEqualByComparingTo("23800.00");
            assertThat(leido.getOpenAccount()).isEqualTo(LA_CUENTA);
            assertThat(leido.getCreatedBy().name()).isEqualTo("Ana Ruiz");
            assertThat(leido.isEnabled()).isTrue();
            assertThat(leido.isVoided()).isFalse();
        }

        @Test
        @DisplayName("las tres referencias opcionales en null se guardan como null")
        void las_tres_referencias_opcionales_en_null_se_guardan_como_null() {
            // impuesto, anulador y —de las seis del save— la unica combinacion de nulos
            // que la base admite: created_by_id es NOT NULL.
            ProductChargeOpenAccount guardado = cargo(LA_CUENTA, COLLAR_SIN_IMPUESTO, 1, null);

            entityManager.flush();
            entityManager.clear();
            ProductChargeOpenAccount leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getTax()).isNull();
            assertThat(leido.isHasTax()).isFalse();
            assertThat(leido.getTaxPercentage()).isNull();
            assertThat(leido.getTaxName()).isNull();
            assertThat(leido.getVoidedBy()).isNull();
            assertThat(leido.getVoidedAt()).isNull();
            assertThat(leido.getVoidReason()).isNull();
            assertThat(leido.getBaseAmount()).isEqualByComparingTo("5000.00");
            assertThat(leido.getTaxAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("el precio congelado no sigue al catalogo cuando el producto se reprecia")
        void el_precio_congelado_no_sigue_al_catalogo() {
            ProductChargeOpenAccount guardado = dosUnidadesConIva();
            entityManager.flush();

            entityManager.createNativeQuery("UPDATE products SET sale_price = 1.00 WHERE id = :id")
                    .setParameter("id", SchemaSeed.PRODUCT_ID).executeUpdate();
            entityManager.clear();

            ProductChargeOpenAccount leido = repository
                    .findByIdAndCompanyId(guardado.getId(), COMPANY).orElseThrow();

            // Reprecar el catalogo no puede mover el total de una cuenta ya cobrada. El
            // ref del producto SI refleja el catalogo vigente, y esa diferencia es la
            // prueba de que lo congelado es real.
            assertThat(leido.getUnitPrice()).isEqualByComparingTo("11900");
            assertThat(leido.getTotalAmount()).isEqualByComparingTo("23800.00");
            assertThat(leido.getProduct().salePrice()).as("el ref sale del catalogo, ya repreciado")
                    .isEqualByComparingTo("1.00");
        }

        @Test
        @DisplayName("la anulacion persiste quien, cuando y por que sin dar de baja la fila")
        void la_anulacion_persiste_quien_cuando_y_por_que() {
            ProductChargeOpenAccount guardado = dosUnidadesConIva();
            entityManager.flush();

            ProductChargeOpenAccount recuperado = repository.findById(guardado.getId())
                    .orElseThrow();
            recuperado.voidCharge(SUPERVISOR, "Se cobro por error");
            repository.save(recuperado);
            entityManager.flush();
            entityManager.clear();

            ProductChargeOpenAccount leido = repository
                    .findByIdAndCompanyId(guardado.getId(), COMPANY).orElseThrow();

            assertThat(leido.isVoided()).isTrue();
            assertThat(leido.isEnabled()).as("anular no da de baja la fila").isTrue();
            assertThat(leido.getVoidedBy().name()).isEqualTo("Luis Paz");
            assertThat(leido.getVoidedAt()).isNotNull();
            assertThat(leido.getVoidReason()).isEqualTo("Se cobro por error");
        }
    }

    @Nested
    @DisplayName("referencias perezosas que solo fallan contra la base")
    class ReferenciasPerezosas {

        @Test
        @DisplayName("un impuesto inexistente no revienta en getReferenceById sino en la FK")
        void un_impuesto_inexistente_revienta_en_la_fk() {
            // getReferenceById devuelve un proxy sin consultar: el id fantasma pasa por
            // el adaptador sin protestar y solo lo caza la clave foranea al insertar.
            assertThatThrownBy(() -> {
                cargo(LA_CUENTA, ALIMENTO_CON_IVA_FANTASMA, 1, null);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("fk_product_charge_open_accounts_tax");
        }

        @Test
        @DisplayName("un cargo sin empleado autor no se puede persistir: created_by_id es NOT NULL")
        void un_cargo_sin_empleado_autor_no_se_puede_persistir() {
            // La rama `createdBy == null` del adaptador existe y el dominio la permite,
            // pero la columna es NOT NULL: ese camino nunca puede terminar en una fila.
            assertThatThrownBy(() -> {
                repository.save(ProductChargeOpenAccount.create(FIRULAIS, ALIMENTO_CON_IVA, 1,
                        LA_CUENTA, null, null));
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("created_by_id");
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa a traves de la cuenta")
    class Tenancy {

        @Test
        @DisplayName("un cargo de otra empresa no se lee por id con la empresa propia")
        void un_cargo_de_otra_empresa_no_se_lee_por_id() {
            ProductChargeOpenAccount ajeno = cargo(LA_CUENTA_AJENA, COLLAR_SIN_IMPUESTO, 1, null);
            entityManager.flush();
            entityManager.clear();

            // El cargo no guarda company_id: la empresa se alcanza atravesando
            // open_accounts. Ese salto de dos tablas es el que un doble no puede falsear.
            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), COMPANY)).isEmpty();
            assertThat(repository.findByIdAndCompanyId(ajeno.getId(), OTRA_COMPANY)).isPresent();
        }

        @Test
        @DisplayName("el listado paginado por empresa no ve los cargos ajenos")
        void el_listado_por_empresa_no_ve_los_cargos_ajenos() {
            ProductChargeOpenAccount propio = dosUnidadesConIva();
            cargo(LA_CUENTA_AJENA, COLLAR_SIN_IMPUESTO, 1, null);
            entityManager.flush();
            entityManager.clear();

            PageResult<ProductChargeOpenAccount> pagina = repository.findAllByCompanyId(COMPANY, 0,
                    20);

            assertThat(pagina.content()).extracting(ProductChargeOpenAccount::getId)
                    .containsExactly(propio.getId());
            assertThat(pagina.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("el detalle de una cuenta ajena no se lista con la empresa propia")
        void el_detalle_de_una_cuenta_ajena_no_se_lista() {
            cargo(LA_CUENTA_AJENA, COLLAR_SIN_IMPUESTO, 1, null);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByOpenAccountIdAndCompanyId(CUENTA_AJENA, COMPANY)).isEmpty();
            assertThat(repository.findByOpenAccountIdAndCompanyId(CUENTA_AJENA, OTRA_COMPANY))
                    .hasSize(1);
        }

        @Test
        @DisplayName("el detalle de una cuenta solo trae los cargos de esa cuenta")
        void el_detalle_de_una_cuenta_solo_trae_sus_cargos() {
            ProductChargeOpenAccount deLaCuenta = dosUnidadesConIva();
            cargo(LA_OTRA_CUENTA, COLLAR_SIN_IMPUESTO, 1, null);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByOpenAccountIdAndCompanyId(CUENTA, COMPANY))
                    .extracting(ProductChargeOpenAccount::getId)
                    .containsExactly(deLaCuenta.getId());
        }
    }

    @Nested
    @DisplayName("paginacion con desempate por id descendente")
    class Paginacion {

        @Test
        @DisplayName("dos paginas consecutivas no repiten ni omiten filas")
        void dos_paginas_consecutivas_no_repiten_ni_omiten_filas() {
            ProductChargeOpenAccount primero = dosUnidadesConIva();
            ProductChargeOpenAccount segundo = cargo(LA_CUENTA, COLLAR_SIN_IMPUESTO, 1, null);
            ProductChargeOpenAccount tercero = cargo(LA_OTRA_CUENTA, ALIMENTO_CON_IVA, 1, null);
            entityManager.flush();
            entityManager.clear();

            PageResult<ProductChargeOpenAccount> pagina0 = repository.findAllByCompanyId(COMPANY, 0,
                    2);
            PageResult<ProductChargeOpenAccount> pagina1 = repository.findAllByCompanyId(COMPANY, 1,
                    2);

            // Sin orden explicito MySQL no garantiza reparto estable y una misma fila
            // puede salir en las dos paginas —o en ninguna—.
            assertThat(pagina0.content()).extracting(ProductChargeOpenAccount::getId)
                    .containsExactly(tercero.getId(), segundo.getId());
            assertThat(pagina1.content()).extracting(ProductChargeOpenAccount::getId)
                    .containsExactly(primero.getId());
            assertThat(pagina0.totalElements()).isEqualTo(3L);
            assertThat(pagina0.totalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("el orden declarado es id descendente: primero lo mas reciente")
        void el_orden_declarado_es_id_descendente() {
            ProductChargeOpenAccount primero = dosUnidadesConIva();
            ProductChargeOpenAccount segundo = cargo(LA_CUENTA, COLLAR_SIN_IMPUESTO, 1, null);
            ProductChargeOpenAccount tercero = cargo(LA_CUENTA, ALIMENTO_CON_IVA, 3, null);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(COMPANY, 0, 20).content())
                    .extracting(ProductChargeOpenAccount::getId)
                    .containsExactly(tercero.getId(), segundo.getId(), primero.getId());
        }

        @Test
        @DisplayName("una pagina fuera de rango vuelve vacia sin perder los totales")
        void una_pagina_fuera_de_rango_vuelve_vacia() {
            dosUnidadesConIva();
            entityManager.flush();
            entityManager.clear();

            PageResult<ProductChargeOpenAccount> pagina = repository.findAllByCompanyId(COMPANY, 5,
                    20);

            assertThat(pagina.content()).isEmpty();
            assertThat(pagina.totalElements()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("idempotencia por cuenta abierta")
    class Idempotencia {

        @Test
        @DisplayName("encuentra el cargo ya registrado con esa clave en esa cuenta")
        void encuentra_el_cargo_ya_registrado_con_esa_clave() {
            ProductChargeOpenAccount guardado = cargo(LA_CUENTA, ALIMENTO_CON_IVA, 1, CLAVE);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByOpenAccountIdAndClientRequestId(CUENTA, CLAVE))
                    .map(ProductChargeOpenAccount::getId).contains(guardado.getId());
        }

        @Test
        @DisplayName("la misma clave en otra cuenta abierta es otro cargo distinto")
        void la_misma_clave_en_otra_cuenta_es_otro_cargo() {
            // El indice unico es (open_account_id, client_request_id): acotarlo por
            // cuenta no puede significar dejar de deduplicar dentro de la cuenta.
            ProductChargeOpenAccount enLaCuenta = cargo(LA_CUENTA, ALIMENTO_CON_IVA, 1, CLAVE);
            ProductChargeOpenAccount enLaOtra = cargo(LA_OTRA_CUENTA, ALIMENTO_CON_IVA, 1, CLAVE);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByOpenAccountIdAndClientRequestId(CUENTA, CLAVE))
                    .map(ProductChargeOpenAccount::getId).contains(enLaCuenta.getId());
            assertThat(repository.findByOpenAccountIdAndClientRequestId(OTRA_CUENTA, CLAVE))
                    .map(ProductChargeOpenAccount::getId).contains(enLaOtra.getId());
            assertThat(enLaCuenta.getId()).isNotEqualTo(enLaOtra.getId());
        }

        @Test
        @DisplayName("varios cargos sin clave conviven: MySQL admite multiples NULL")
        void varios_cargos_sin_clave_conviven() {
            ProductChargeOpenAccount uno = cargo(LA_CUENTA, ALIMENTO_CON_IVA, 1, null);
            ProductChargeOpenAccount otro = cargo(LA_CUENTA, ALIMENTO_CON_IVA, 1, null);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByOpenAccountIdAndCompanyId(CUENTA, COMPANY))
                    .extracting(ProductChargeOpenAccount::getId)
                    .containsExactlyInAnyOrder(uno.getId(), otro.getId());
        }

        @Test
        @DisplayName("repetir la clave dentro de la misma cuenta sigue siendo conflicto")
        void repetir_la_clave_dentro_de_la_misma_cuenta_es_conflicto() {
            cargo(LA_CUENTA, ALIMENTO_CON_IVA, 1, CLAVE);
            entityManager.flush();

            // El respaldo de la carrera concurrente del check-then-act: la 2a insercion
            // con el mismo par la rechaza la base, no el service.
            assertThatThrownBy(() -> {
                cargo(LA_CUENTA, COLLAR_SIN_IMPUESTO, 1, CLAVE);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("uq_product_charge_open_accounts_request");
        }
    }

    @Nested
    @DisplayName("baja logica")
    class BajaLogica {

        @Test
        @DisplayName("borrar esconde la fila de las lecturas JPA pero no la elimina")
        void borrar_esconde_la_fila_pero_no_la_elimina() {
            ProductChargeOpenAccount guardado = dosUnidadesConIva();
            entityManager.flush();

            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).isEmpty();
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), COMPANY)).isEmpty();
            assertThat(filasCrudas(guardado.getId())).as("@SQLDelete solo apaga enabled")
                    .isEqualTo(1L);
        }

        @Test
        @DisplayName("findAll sin argumentos tampoco devuelve las filas dadas de baja")
        void find_all_no_devuelve_las_filas_dadas_de_baja() {
            ProductChargeOpenAccount vivo = dosUnidadesConIva();
            ProductChargeOpenAccount muerto = cargo(LA_CUENTA, COLLAR_SIN_IMPUESTO, 1, null);
            entityManager.flush();

            repository.delete(muerto.getId());
            entityManager.flush();
            entityManager.clear();

            // findAll() no acota empresa —es la variante ancha— pero el @SQLRestriction
            // si se aplica: la fila apagada no sale por ningun camino JPA.
            List<Long> ids = repository.findAll().stream().map(ProductChargeOpenAccount::getId)
                    .toList();
            assertThat(ids).contains(vivo.getId()).doesNotContain(muerto.getId());
        }

        @Test
        @DisplayName("la fila dada de baja sigue ocupando la clave unica siendo invisible")
        void la_fila_dada_de_baja_sigue_ocupando_la_clave_unica() {
            ProductChargeOpenAccount guardado = cargo(LA_CUENTA, ALIMENTO_CON_IVA, 1, CLAVE);
            entityManager.flush();

            repository.delete(guardado.getId());
            entityManager.flush();

            // uq_product_charge_open_accounts_request NO incluye enabled: la fila
            // apagada ya no se lee —findByOpenAccountIdAndClientRequestId da vacio— pero
            // sigue reservando el par, asi que reintentar el mismo cargo revienta en vez
            // de deduplicar. Detectarlo exige base real: es SQL, no logica Java.
            assertThat(repository.findByOpenAccountIdAndClientRequestId(CUENTA, CLAVE)).isEmpty();
            assertThatThrownBy(() -> {
                cargo(LA_CUENTA, ALIMENTO_CON_IVA, 1, CLAVE);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("uq_product_charge_open_accounts_request");
        }
    }
}
