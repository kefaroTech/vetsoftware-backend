package com.vetsoftware.app.testsupport;

import jakarta.persistence.EntityManager;

/**
 * Siembra las filas raiz que exigen las claves foraneas del schema real.
 *
 * <p>
 * <b>Por que hace falta.</b> El dominio de varias features referencia a otras
 * por id plano ({@code companyId}, {@code branchId}, {@code productId}) sin
 * entidad JPA asociada, asi que leyendo el codigo parece que se puede insertar
 * un lote con {@code companyId = 1} y ya. La base dice otra cosa: hay FK reales
 * a {@code companies}, {@code branches} y {@code products}, y esas a su vez
 * cuelgan de ciudad y categoria. La cadena completa es
 * {@code pais → departamento → ciudad → empresa → contrato → linea de contrato → permisos derivados},
 * y en paralelo
 * {@code modulo → submodulo → articulo de catalogo → lista de precios → precio};
 * a partir de la empresa cuelgan {@code sede → categoria → producto},
 * {@code empleado} y {@code terminal}. Sin ella cualquier {@code @DataJpaTest}
 * de inventario, caja o cuenta abierta muere con un
 * {@code DataIntegrityViolation}.
 *
 * <p>
 * <b>La membresia ya no existe.</b> {@code memberships} y
 * {@code membership_sub_modules} desaparecieron con el modelo de suscripciones;
 * {@code companies} ya no tiene {@code membership_id}. Lo que ahora decide que
 * puede usar una empresa es su contrato, y por eso el seed crea para las dos
 * empresas una {@code subscriptions} vigente con su linea y sus
 * {@code company_entitlements}: la invariante del modelo dice que <b>no existe
 * empresa sin contrato vigente</b>, y la empresa ajena tambien lo lleva porque
 * los tests de aislamiento entre tenants necesitan que sea utilizable, no un
 * cascaron.
 *
 * <p>
 * <b>Solo columnas obligatorias.</b> Se insertan por SQL nativo, no por
 * entidades JPA, para no arrastrar las invariantes de siete agregados ajenos
 * dentro de un test que solo quiere un lote. Los ids son fijos y explicitos
 * ({@link #COMPANY_ID} y compañia) para que las aserciones puedan nombrarlos.
 *
 * <p>
 * <b>Nada de {@code INSERT IGNORE}.</b> Silencia tambien los errores de clave
 * foranea, de {@code CHECK} y de columna {@code NOT NULL} omitida: el seed
 * "funciona", no inserta nada, y el test falla cuarenta lineas despues con un
 * {@code EntityNotFound} incomprensible. Engaño tres veces el 2026-08-17 y esta
 * documentado como anti-patron en el {@code CLAUDE.md}. La idempotencia se
 * consigue con {@code ON DUPLICATE KEY UPDATE id = id}, que solo perdona la
 * colision de clave y deja que todo lo demas reviente ruidosamente.
 *
 * <p>
 * <b>Columnas generadas.</b> {@code subscriptions.active_marker} y
 * {@code subscription_items.current_item_marker} son {@code GENERATED ALWAYS}:
 * MySQL devuelve {@code ERROR 3105} si se nombran en el {@code INSERT}, aunque
 * el valor sea {@code NULL}. Estan deliberadamente ausentes; no las añadas.
 *
 * <p>
 * <b>Ninguna tabla de dinero.</b> Ni documentos de cobro, ni cargos, ni pagos:
 * este seed existe para satisfacer claves foraneas, no para montar un escenario
 * de negocio. Sembrarlas haria que cualquier asercion de "la cartera de esta
 * clinica es cero" empezara en falso.
 *
 * <p>
 * Cada rodaja {@code @DataJpaTest} corre en su propia transaccion con rollback,
 * asi que hay que llamar a {@link #seed(EntityManager)} en el
 * {@code @BeforeEach} de cada test que lo necesite.
 */
public final class SchemaSeed {

    public static final Long COUNTRY_ID = 900L;
    public static final Long STATE_ID = 900L;
    public static final Long CITY_ID = 900L;
    public static final Long COMPANY_ID = 900L;
    public static final Long OTRA_COMPANY_ID = 901L;
    public static final Long BRANCH_ID = 910L;
    public static final Long OTRA_BRANCH_ID = 911L;
    public static final Long CATEGORY_ID = 920L;
    public static final Long PRODUCT_ID = 930L;
    public static final Long OTRO_PRODUCT_ID = 931L;
    public static final Long EMPLOYEE_ID = 940L;
    public static final Long OTRO_EMPLOYEE_ID = 941L;
    public static final Long TERMINAL_ID = 950L;
    public static final Long OTRO_TERMINAL_ID = 951L;

    /** Catalogo comercial minimo: un articulo nucleo con su precio publicado. */
    public static final Long CATALOG_ITEM_CORE_ID = 960L;
    public static final Long CATALOG_PRICE_CORE_ID = 961L;
    public static final Long PRICE_LIST_ID = 962L;
    public static final Long CATALOG_ITEM_SUB_MODULE_ID = 963L;

    /** Un contrato vigente por empresa, y su linea. */
    public static final Long SUBSCRIPTION_ID = 970L;
    public static final Long OTRA_SUBSCRIPTION_ID = 971L;
    public static final Long SUBSCRIPTION_ITEM_ID = 972L;
    public static final Long OTRO_SUBSCRIPTION_ITEM_ID = 973L;

    /** Los permisos derivados del contrato, y el techo contratado. */
    public static final Long ENTITLEMENT_ID = 974L;
    public static final Long OTRO_ENTITLEMENT_ID = 975L;
    public static final Long CAPACITY_ID = 976L;

    /** Submodulo de prueba y su modulo padre, con ids estables. */
    public static final Long SUB_MODULE_ID = 980L;
    public static final Long MODULE_ID = 981L;

    /**
     * {@code chk_price_lists_published} exige {@code published_at} Y
     * {@code published_by_system_user_id} en cuanto el estado deja de ser
     * {@code DRAFT}, asi que hace falta un usuario de sistema. Dejar la lista en
     * borrador no sirve: un contrato apunta a la lista con la que se firmo, y
     * firmar contra un borrador es justo lo que el modelo prohibe.
     */
    public static final Long SYSTEM_USER_ID = 990L;

    private static final String INICIO = "2026-01-01";
    private static final String INICIO_TS = "2026-01-01 00:00:00.000000";

    private SchemaSeed() {
    }

    /** Inserta la cadena completa. Idempotente dentro de la misma transaccion. */
    public static void seed(EntityManager em) {
        geografia(em);
        arbolDeModulos(em);
        usuarioDeSistema(em);
        catalogoComercial(em);

        company(em, COMPANY_ID, "Veterinaria de prueba", "900123456");
        company(em, OTRA_COMPANY_ID, "Veterinaria ajena", "900654321");

        contrato(em, SUBSCRIPTION_ID, "SUS-TEST-000900", COMPANY_ID);
        contrato(em, OTRA_SUBSCRIPTION_ID, "SUS-TEST-000901", OTRA_COMPANY_ID);
        lineaDeContrato(em, SUBSCRIPTION_ITEM_ID, COMPANY_ID, SUBSCRIPTION_ID);
        lineaDeContrato(em, OTRO_SUBSCRIPTION_ITEM_ID, OTRA_COMPANY_ID, OTRA_SUBSCRIPTION_ID);
        permisoDerivado(em, ENTITLEMENT_ID, COMPANY_ID, SUBSCRIPTION_ID, SUBSCRIPTION_ITEM_ID);
        permisoDerivado(em, OTRO_ENTITLEMENT_ID, OTRA_COMPANY_ID, OTRA_SUBSCRIPTION_ID,
                OTRO_SUBSCRIPTION_ITEM_ID);
        capacidad(em);

        branch(em, BRANCH_ID, "Sede Centro", "CENTRO", COMPANY_ID);
        branch(em, OTRA_BRANCH_ID, "Sede Norte", "NORTE", COMPANY_ID);

        insert(em, """
                INSERT INTO product_categories (id, name, description, company_id)
                VALUES (:id, 'Medicamentos', 'Categoria de prueba', %d)
                ON DUPLICATE KEY UPDATE id = id
                """.formatted(COMPANY_ID), CATEGORY_ID);
        product(em, PRODUCT_ID, "Amoxicilina 500mg", "SKU-100");
        product(em, OTRO_PRODUCT_ID, "Ivermectina 1%", "SKU-200");

        empleado(em, EMPLOYEE_ID, "EMP-001", "Ana Ruiz", "ana@test.local");
        empleado(em, OTRO_EMPLOYEE_ID, "EMP-002", "Luis Paz", "luis@test.local");
        terminal(em, TERMINAL_ID, "Caja principal", "PRINCIPAL", BRANCH_ID);
        terminal(em, OTRO_TERMINAL_ID, "Caja 2", "CAJA-2", BRANCH_ID);
        em.flush();
    }

    private static void geografia(EntityManager em) {
        insert(em, """
                INSERT INTO countries (id, name, created_date, enabled)
                VALUES (:id, 'Pais de prueba', NOW(), true)
                ON DUPLICATE KEY UPDATE id = id
                """, COUNTRY_ID);
        insert(em, """
                INSERT INTO states (id, name, country_id, created_date, enabled)
                VALUES (:id, 'Antioquia', %d, NOW(), true)
                ON DUPLICATE KEY UPDATE id = id
                """.formatted(COUNTRY_ID), STATE_ID);
        insert(em, """
                INSERT INTO cities (id, name, state_id, created_date, enabled)
                VALUES (:id, 'Medellin', %d, NOW(), true)
                ON DUPLICATE KEY UPDATE id = id
                """.formatted(STATE_ID), CITY_ID);
    }

    /**
     * Modulo y submodulo propios: {@code catalog_item_sub_modules} y
     * {@code company_entitlements} referencian {@code sub_modules}, y los cuatro
     * que siembran las migraciones ({@code BRANCH}, {@code INVENTORY},
     * {@code CASH}, {@code PURCHASES}) no tienen id estable.
     */
    private static void arbolDeModulos(EntityManager em) {
        insert(em, """
                INSERT INTO modules (id, name, code, created_date, enabled, version)
                VALUES (:id, 'Modulo de prueba', 'TEST_MODULE', NOW(), true, 0)
                ON DUPLICATE KEY UPDATE id = id
                """, MODULE_ID);
        insert(em, """
                INSERT INTO sub_modules (id, name, code, module_id, created_date, enabled, version,
                                         is_sellable, read_only_capable)
                VALUES (:id, 'Submodulo de prueba', 'TEST_SUB_MODULE', %d, NOW(), true, 0,
                        true, true)
                ON DUPLICATE KEY UPDATE id = id
                """.formatted(MODULE_ID), SUB_MODULE_ID);
    }

    private static void usuarioDeSistema(EntityManager em) {
        insert(em, """
                INSERT INTO system_users (id, code, hash_password, created_date, enabled, version)
                VALUES (:id, 'SEED-SYSTEM', 'x', NOW(), true, 0)
                ON DUPLICATE KEY UPDATE id = id
                """, SYSTEM_USER_ID);
    }

    private static void catalogoComercial(EntityManager em) {
        insert(em, """
                INSERT INTO catalog_items (id, code, name, item_type, is_core, min_quantity,
                                           max_quantity, sort_order, status, created_date, enabled,
                                           version)
                VALUES (:id, 'CORE', 'Nucleo de prueba', 'MODULE', true, 1, 1, 0, 'ACTIVE', NOW(),
                        true, 0)
                ON DUPLICATE KEY UPDATE id = id
                """, CATALOG_ITEM_CORE_ID);
        insert(em, """
                INSERT INTO catalog_item_sub_modules (id, catalog_item_id, sub_module_id,
                                                      created_date, enabled)
                VALUES (:id, %d, %d, NOW(), true)
                ON DUPLICATE KEY UPDATE id = id
                """.formatted(CATALOG_ITEM_CORE_ID, SUB_MODULE_ID), CATALOG_ITEM_SUB_MODULE_ID);
        insert(em, """
                INSERT INTO price_lists (id, code, name, currency, valid_from, status,
                                         published_at, published_by_system_user_id,
                                         created_date, enabled, version)
                VALUES (:id, 'LISTA-TEST', 'Lista de prueba', 'COP', '%s', 'PUBLISHED',
                        '%s', %d, NOW(), true, 0)
                ON DUPLICATE KEY UPDATE id = id
                """.formatted(INICIO, INICIO + " 00:00:00", SYSTEM_USER_ID), PRICE_LIST_ID);
        insert(em, """
                INSERT INTO catalog_prices (id, price_list_id, catalog_item_id, billing_cycle,
                                            tier_min, tier_max, included_quantity, unit_amount,
                                            setup_amount, tax_rate, tax_treatment,
                                            created_date, enabled, version)
                VALUES (:id, %d, %d, 'MONTHLY', 1, NULL, 2, 100000.00, 0.00, 19.00, 'TAXED',
                        NOW(), true, 0)
                ON DUPLICATE KEY UPDATE id = id
                """.formatted(PRICE_LIST_ID, CATALOG_ITEM_CORE_ID), CATALOG_PRICE_CORE_ID);
    }

    private static void company(EntityManager em, Long id, String nombre, String nit) {
        insert(em, """
                INSERT INTO companies (id, name, identifier, city_id)
                VALUES (:id, '%s', '%s', %d)
                ON DUPLICATE KEY UPDATE id = id
                """.formatted(nombre, nit, CITY_ID), id);
    }

    /**
     * {@code active_marker} es {@code GENERATED ALWAYS} y no se nombra: impone un
     * solo contrato activo por empresa y lo calcula el motor.
     */
    private static void contrato(EntityManager em, Long id, String numero, Long companyId) {
        insert(em, """
                INSERT INTO subscriptions (id, subscription_number, company_id, quote_id,
                                           price_list_id, billing_cycle, status, start_date,
                                           trial_end_date, current_period_start,
                                           current_period_end, next_billing_date,
                                           commitment_end_date, grace_days, past_due_since,
                                           auto_renew, created_date, enabled, version)
                VALUES (:id, '%s', %d, NULL, %d, 'MONTHLY', 'ACTIVE', '%s', NULL,
                        '%s', '2026-01-31', '2026-02-01', NULL, 5, NULL, true, NOW(), true, 0)
                ON DUPLICATE KEY UPDATE id = id
                """.formatted(numero, companyId, PRICE_LIST_ID, INICIO, INICIO), id);
    }

    /** {@code effective_to} nulo = linea vigente. */
    private static void lineaDeContrato(EntityManager em, Long id, Long companyId,
            Long subscriptionId) {
        insert(em, """
                INSERT INTO subscription_items (id, company_id, subscription_id, catalog_item_id,
                                                item_code, item_name, item_type, capacity_unit,
                                                included_quantity, tax_treatment, quantity,
                                                unit_amount, tax_rate, effective_from,
                                                effective_to, origin, created_amendment_id,
                                                ended_amendment_id, created_date, enabled, version)
                VALUES (:id, %d, %d, %d, 'CORE', 'Nucleo de prueba', 'MODULE', NULL,
                        2, 'TAXED', 1, 100000.00, 19.00, '%s', NULL, 'INITIAL', NULL, NULL,
                        NOW(), true, 0)
                ON DUPLICATE KEY UPDATE id = id
                """.formatted(companyId, subscriptionId, CATALOG_ITEM_CORE_ID, INICIO), id);
    }

    /**
     * Sin esta tabla cualquier gate de entitlement deja fuera al test.
     * {@code company_entitlements} NO tiene columna {@code enabled} ni
     * {@code version}: se recalcula borrando y reinsertando.
     */
    private static void permisoDerivado(EntityManager em, Long id, Long companyId,
            Long subscriptionId, Long subscriptionItemId) {
        insert(em, """
                INSERT INTO company_entitlements (id, company_id, sub_module_id, access_level,
                                                  source, subscription_id, subscription_item_id,
                                                  valid_from, valid_until, recalculated_at,
                                                  created_date)
                VALUES (:id, %d, %d, 'FULL', 'SUBSCRIPTION', %d, %d, '%s', NULL, '%s', NOW())
                ON DUPLICATE KEY UPDATE id = id
                """.formatted(companyId, SUB_MODULE_ID, subscriptionId, subscriptionItemId,
                INICIO_TS, INICIO_TS), id);
    }

    private static void capacidad(EntityManager em) {
        insert(em, """
                INSERT INTO company_capacities (id, company_id, capacity_unit, limit_quantity,
                                                used_quantity, subscription_id, recalculated_at,
                                                created_date)
                VALUES (:id, %d, 'USER', 2, 0, %d, '%s', NOW())
                ON DUPLICATE KEY UPDATE id = id
                """.formatted(COMPANY_ID, SUBSCRIPTION_ID, INICIO_TS), CAPACITY_ID);
    }

    private static void empleado(EntityManager em, Long id, String codigo, String nombre,
            String correo) {
        insert(em, """
                INSERT INTO employees (id, employee_code, hash_password, name, email, company_id)
                VALUES (:id, '%s', 'x', '%s', '%s', %d)
                ON DUPLICATE KEY UPDATE id = id
                """.formatted(codigo, nombre, correo, COMPANY_ID), id);
    }

    private static void terminal(EntityManager em, Long id, String nombre, String codigo,
            Long branchId) {
        insert(em, """
                INSERT INTO cash_terminals (id, company_id, branch_id, name, code, active,
                                            created_at)
                VALUES (:id, %d, %d, '%s', '%s', true, '2026-01-15 08:00:00')
                ON DUPLICATE KEY UPDATE id = id
                """.formatted(COMPANY_ID, branchId, nombre, codigo), id);
    }

    private static void branch(EntityManager em, Long id, String nombre, String codigo,
            Long companyId) {
        insert(em, """
                INSERT INTO branches (id, name, code, city_id, company_id)
                VALUES (:id, '%s', '%s', %d, %d)
                ON DUPLICATE KEY UPDATE id = id
                """.formatted(nombre, codigo, CITY_ID, companyId), id);
    }

    private static void product(EntityManager em, Long id, String nombre, String codigo) {
        insert(em, """
                INSERT INTO products (id, name, code, sale_price, product_category_id,
                                      company_id, tax_treatment)
                VALUES (:id, '%s', '%s', 10000.00, %d, %d, 'GRAVADO')
                ON DUPLICATE KEY UPDATE id = id
                """.formatted(nombre, codigo, CATEGORY_ID, COMPANY_ID), id);
    }

    private static void insert(EntityManager em, String sql, Long id) {
        em.createNativeQuery(sql).setParameter("id", id).executeUpdate();
    }
}
