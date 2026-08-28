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
 * <b>Este andamio revienta, no perdona.</b> Ni {@code INSERT IGNORE} ni
 * {@code ON DUPLICATE KEY UPDATE}: todos los {@code INSERT} de aqui son
 * {@code INSERT} pelados. El {@code IGNORE} silencia tambien los errores de
 * clave foranea, de {@code CHECK} y de columna {@code NOT NULL} omitida —engaño
 * tres veces el 2026-08-17 y esta documentado como anti-patron en el
 * {@code CLAUDE.md}—, y el {@code ON DUPLICATE KEY UPDATE id = id} que lo
 * sustituyo resulto ser la misma trampa un escalon mas abajo: perdona
 * exactamente la colision que hay que ver. Lo demostro la incidencia #647. El
 * changeset 308 empezo a sembrar el catalogo comercial real —con
 * {@code code = 'CORE'}— en todos los entornos, y el {@code INSERT} de este
 * andamio, que sembraba su propio {@code 'CORE'} con id fijo, paso a chocar
 * contra el indice unico de {@code catalog_items.code}. La clausula lo
 * convirtio en un no-op: el andamio creia haber sembrado, el id 960 no existia,
 * y las 93 rodajas de integracion morian despues con violaciones de clave
 * foranea en tablas hijas que no tenian nada que ver.
 *
 * <p>
 * La idempotencia no hacia falta para nada: cada {@code @DataJpaTest} corre en
 * su propia transaccion con rollback y llama a {@link #seed(EntityManager)} una
 * sola vez. Lo unico que compraba la clausula era el silencio.
 *
 * <p>
 * <b>Lo que ya siembra Liquibase no se resiembra: se resuelve.</b> El catalogo
 * comercial (changesets 308-313) y los ejes limitables llegan poblados en
 * <em>todos</em> los entornos, incluido el contenedor de test. Duplicarlos aqui
 * bajo los mismos codigos es lo que produjo #647, y hacerlo bajo codigos
 * propios seria peor todavia: las consultas de produccion buscan por
 * {@code code = 'CORE'} —ver
 * {@code PlatformCatalogTemplateJpaRepository.findInitialContractTemplate}—,
 * asi que un {@code 'TEST-CORE'} paralelo dejaria a los tests afirmando sobre
 * una fila que produccion no mira. Por eso
 * {@link #catalogItemId(EntityManager, String)} y
 * {@link #limitDimensionId(EntityManager, String)} <em>resuelven</em> el id de
 * la fila real en vez de inventar uno.
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

    /**
     * Tarifa de laboratorio con el precio del nucleo. El articulo {@code CORE} ya
     * NO se siembra aqui —lo trae el changeset 308 y se resuelve con
     * {@link #catalogItemId(EntityManager, String)}—, pero su tramo de precio si:
     * {@code LISTA-TEST} es una lista propia, con un unico tramo y contenido
     * conocido, que es lo que permite a las rodajas de {@code pricelist} afirmar
     * «esta lista tiene dos precios». Colgar esas aserciones de
     * {@code LISTA-2026-01}, que trae 64 filas x 2 ciclos, seria atarlas al
     * catalogo comercial.
     */
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

    /**
     * Cotización aceptada de cada empresa. Existe porque
     * {@code company_trial_windows.source_quote_id} va {@code NOT NULL} (D-55):
     * todo cliente nace con su ventana y el único camino de alta es cotizar, firmar
     * y abrirla. Sin esta fila, ninguna rodaja de la capa I puede insertar nada.
     */
    public static final Long QUOTE_ID = 992L;
    public static final Long OTRA_QUOTE_ID = 993L;

    private static final String INICIO = "2026-01-01";
    private static final String INICIO_TS = "2026-01-01 00:00:00.000000";

    private SchemaSeed() {
    }

    /** Inserta la cadena completa. Idempotente dentro de la misma transaccion. */
    public static void seed(EntityManager em) {
        geografia(em);
        arbolDeModulos(em);
        usuarioDeSistema(em);
        Long nucleo = catalogItemId(em, "CORE");
        catalogoComercial(em, nucleo);

        company(em, COMPANY_ID, "Veterinaria de prueba", "900123456");
        company(em, OTRA_COMPANY_ID, "Veterinaria ajena", "900654321");

        cotizacionAceptada(em, QUOTE_ID, "COT-TEST-000900", COMPANY_ID);
        cotizacionAceptada(em, OTRA_QUOTE_ID, "COT-TEST-000901", OTRA_COMPANY_ID);

        contrato(em, SUBSCRIPTION_ID, "SUS-TEST-000900", COMPANY_ID);
        contrato(em, OTRA_SUBSCRIPTION_ID, "SUS-TEST-000901", OTRA_COMPANY_ID);
        lineaDeContrato(em, SUBSCRIPTION_ITEM_ID, COMPANY_ID, SUBSCRIPTION_ID, nucleo);
        lineaDeContrato(em, OTRO_SUBSCRIPTION_ITEM_ID, OTRA_COMPANY_ID, OTRA_SUBSCRIPTION_ID,
                nucleo);
        permisoDerivado(em, ENTITLEMENT_ID, COMPANY_ID, SUBSCRIPTION_ID, SUBSCRIPTION_ITEM_ID);
        permisoDerivado(em, OTRO_ENTITLEMENT_ID, OTRA_COMPANY_ID, OTRA_SUBSCRIPTION_ID,
                OTRO_SUBSCRIPTION_ITEM_ID);
        capacidad(em);

        branch(em, BRANCH_ID, "Sede Centro", "CENTRO", COMPANY_ID);
        branch(em, OTRA_BRANCH_ID, "Sede Norte", "NORTE", COMPANY_ID);

        insert(em, """
                INSERT INTO product_categories (id, name, description, company_id)
                VALUES (:id, 'Medicamentos', 'Categoria de prueba', %d)
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
                """, COUNTRY_ID);
        insert(em, """
                INSERT INTO states (id, name, country_id, created_date, enabled)
                VALUES (:id, 'Antioquia', %d, NOW(), true)
                """.formatted(COUNTRY_ID), STATE_ID);
        insert(em, """
                INSERT INTO cities (id, name, state_id, created_date, enabled)
                VALUES (:id, 'Medellin', %d, NOW(), true)
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
                """, MODULE_ID);
        insert(em, """
                INSERT INTO sub_modules (id, name, code, module_id, created_date, enabled, version,
                                         is_sellable, read_only_capable)
                VALUES (:id, 'Submodulo de prueba', 'TEST_SUB_MODULE', %d, NOW(), true, 0,
                        true, true)
                """.formatted(MODULE_ID), SUB_MODULE_ID);
    }

    private static void usuarioDeSistema(EntityManager em) {
        insert(em, """
                INSERT INTO system_users (id, code, hash_password, created_date, enabled, version)
                VALUES (:id, 'SEED-SYSTEM', 'x', NOW(), true, 0)
                """, SYSTEM_USER_ID);
    }

    /**
     * <b>El articulo {@code CORE} ya no se siembra: llega del changeset 308 y se
     * recibe resuelto por parametro.</b> Lo que queda aqui es lo que el catalogo
     * comercial NO trae y las rodajas necesitan con contenido conocido: el enlace
     * del nucleo al submodulo de prueba y la tarifa de laboratorio.
     */
    private static void catalogoComercial(EntityManager em, Long nucleo) {
        insert(em, """
                INSERT INTO catalog_item_sub_modules (id, catalog_item_id, sub_module_id,
                                                      created_date, enabled)
                VALUES (:id, %d, %d, NOW(), true)
                """.formatted(nucleo, SUB_MODULE_ID), CATALOG_ITEM_SUB_MODULE_ID);
        insert(em, """
                INSERT INTO price_lists (id, code, name, currency, valid_from, status,
                                         published_at, published_by_system_user_id,
                                         created_date, enabled, version)
                VALUES (:id, 'LISTA-TEST', 'Lista de prueba', 'COP', '%s', 'PUBLISHED',
                        '%s', %d, NOW(), true, 0)
                """.formatted(INICIO, INICIO + " 00:00:00", SYSTEM_USER_ID), PRICE_LIST_ID);
        insert(em, """
                INSERT INTO catalog_prices (id, price_list_id, catalog_item_id, billing_cycle,
                                            tier_min, tier_max, included_quantity, unit_amount,
                                            setup_amount, tax_rate, tax_treatment,
                                            created_date, enabled, version)
                VALUES (:id, %d, %d, 'MONTHLY', 1, NULL, 2, 100000.00, 0.00, 19.00, 'TAXED',
                        NOW(), true, 0)
                """.formatted(PRICE_LIST_ID, nucleo), CATALOG_PRICE_CORE_ID);
    }

    private static void company(EntityManager em, Long id, String nombre, String nit) {
        insert(em, """
                INSERT INTO companies (id, name, identifier, city_id)
                VALUES (:id, '%s', '%s', %d)
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
                """.formatted(numero, companyId, PRICE_LIST_ID, INICIO, INICIO), id);
    }

    /** {@code effective_to} nulo = linea vigente. */
    private static void lineaDeContrato(EntityManager em, Long id, Long companyId,
            Long subscriptionId, Long nucleo) {
        insert(em, """
                INSERT INTO subscription_items (id, company_id, subscription_id, catalog_item_id,
                                                item_code, item_name, item_type, capacity_unit,
                                                included_quantity, tax_treatment, quantity,
                                                unit_amount, tax_rate, tier_min, tier_max,
                                                months_in_cycle, charge_mode, trial_eligibility,
                                                max_trial_days, trial_end_date, activation_path,
                                                billing_effect, effective_from,
                                                effective_to, origin, succeeds_item_id,
                                                created_amendment_id,
                                                ended_amendment_id, created_date, enabled, version)
                VALUES (:id, %d, %d, %d, 'CORE', 'Nucleo de prueba', 'MODULE', NULL,
                        2, 'TAXED', 1, 100000.00, 19.00, 1, NULL,
                        1, 'PAID', 'NEVER_FREE', 0, NULL, 'PLATFORM',
                        'NONE', '%s', NULL, 'INITIAL', NULL, NULL, NULL,
                        NOW(), true, 0)
                """.formatted(companyId, subscriptionId, nucleo, INICIO), id);
        composicionCongelada(em, id, companyId);
    }

    /**
     * D-76: la composicion congelada de la linea. Sin ella el recalculo de permisos
     * no devuelve NADA para este contrato, porque desde el changeset 338 lee
     * {@code subscription_item_sub_modules} y ya no el catalogo vivo. El relleno
     * del changeset no alcanza a estas filas: Liquibase corre antes de que el seed
     * las inserte.
     */
    private static void composicionCongelada(EntityManager em, Long subscriptionItemId,
            Long companyId) {
        insert(em, """
                INSERT INTO subscription_item_sub_modules (id, company_id, subscription_item_id,
                                                           sub_module_id, created_date, enabled)
                VALUES (:id, %d, %d, %d, NOW(6), true)
                """.formatted(companyId, subscriptionItemId, SUB_MODULE_ID),
                subscriptionItemId + 40_000L);
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
                """.formatted(companyId, SUB_MODULE_ID, subscriptionId, subscriptionItemId,
                INICIO_TS, INICIO_TS), id);
    }

    /**
     * El contador apunta al eje del catalogo, no a una unidad de una lista cerrada
     * (#629). El id del eje se <em>resuelve</em> igual que el resto de catalogo
     * sembrado por migracion: {@code USER} lo pone el changeset 313 y su
     * {@code measure_kind} es {@code STOCK}, que es lo que la clave foranea
     * compuesta {@code (limit_dimension_id, measure_kind)} exige que coincida.
     *
     * <p>
     * {@code period_key} lleva el centinela porque {@code USER} no es de flujo:
     * dejarlo vacio no daria error visible, dejaria caber dos contadores del mismo
     * eje bajo el indice unico (R-LIMIT-05).
     */
    private static void capacidad(EntityManager em) {
        Long ejeUsuarios = limitDimensionId(em, "USER");
        insert(em, """
                INSERT INTO company_capacities (id, company_id, limit_dimension_id, measure_kind,
                                                period_key, limit_quantity, used_quantity,
                                                subscription_id, limit_recalculated_at,
                                                created_date)
                VALUES (:id, %d, %d, 'STOCK', 'ALLTIME', 2, 0, %d, '%s', NOW())
                """.formatted(COMPANY_ID, ejeUsuarios, SUBSCRIPTION_ID, INICIO_TS), CAPACITY_ID);
    }

    private static void empleado(EntityManager em, Long id, String codigo, String nombre,
            String correo) {
        insert(em, """
                INSERT INTO employees (id, employee_code, hash_password, name, email, company_id)
                VALUES (:id, '%s', 'x', '%s', '%s', %d)
                """.formatted(codigo, nombre, correo, COMPANY_ID), id);
    }

    private static void terminal(EntityManager em, Long id, String nombre, String codigo,
            Long branchId) {
        insert(em, """
                INSERT INTO cash_terminals (id, company_id, branch_id, name, code, active,
                                            created_at)
                VALUES (:id, %d, %d, '%s', '%s', true, '2026-01-15 08:00:00')
                """.formatted(COMPANY_ID, branchId, nombre, codigo), id);
    }

    private static void branch(EntityManager em, Long id, String nombre, String codigo,
            Long companyId) {
        insert(em, """
                INSERT INTO branches (id, name, code, city_id, company_id)
                VALUES (:id, '%s', '%s', %d, %d)
                """.formatted(nombre, codigo, CITY_ID, companyId), id);
    }

    private static void product(EntityManager em, Long id, String nombre, String codigo) {
        insert(em, """
                INSERT INTO products (id, name, code, sale_price, product_category_id,
                                      company_id, tax_treatment)
                VALUES (:id, '%s', '%s', 10000.00, %d, %d, 'GRAVADO')
                """.formatted(nombre, codigo, CATEGORY_ID, COMPANY_ID), id);
    }

    /**
     * Cotización ya aceptada. {@code chk_quotes_accepted} exige la fecha de
     * aceptación en cuanto el estado es {@code ACCEPTED}, y
     * {@code chk_quotes_party} exige empresa o prospecto: aquí va la empresa, que
     * es el caso del cliente que firma.
     */
    /**
     * <b>{@code created_date} fijo, no {@code NOW()}.</b> Es la columna por la que
     * ordena el embudo de cotizaciones ({@code created_date DESC, id DESC}), asi
     * que con la hora de reloj esta fila se colaba SIEMPRE en la cabeza del listado
     * y la posicion de las cotizaciones que escribe cada rodaja dependia del
     * momento en que corriera la suite. Un andamio cuyo orden cambia solo no es un
     * andamio: es un fallo intermitente esperando a la siguiente ejecucion.
     */
    private static void cotizacionAceptada(EntityManager em, Long id, String numero,
            Long companyId) {
        insert(em, """
                INSERT INTO quotes (id, quote_number, company_id, price_list_id, billing_cycle,
                                    subtotal_amount, discount_amount, tax_amount, total_amount,
                                    status, valid_until, trial_days, accepted_at,
                                    client_request_id, created_date, enabled, version)
                VALUES (:id, '%s', %d, %d, 'MONTHLY', 100000.00, 0.00, 19000.00, 119000.00,
                        'ACCEPTED', '2026-12-31', 30, '%s', 'req-%s', '%s', true, 0)
                """.formatted(numero, companyId, PRICE_LIST_ID, INICIO_TS, numero, INICIO_TS), id);
    }

    /**
     * El id del eje limitable que ya siembra el changeset 313.
     *
     * <p>
     * <strong>No se siembra aquí, se resuelve.</strong> {@code limit_dimensions}
     * llega poblada por la migración —los ocho ejes, con sus tipos de medida y su
     * enfriamiento— y {@code code} es único. Insertar aquí un {@code ANIMAL} propio
     * no daría error: chocaría contra esa unicidad y el {@code ON DUPLICATE KEY} lo
     * convertiría en un no-op silencioso, dejando un id que ninguna clave foránea
     * puede alcanzar. Es exactamente la trampa que el {@code CLAUDE.md} describe
     * sobre los seeds que se tragan su propio error.
     *
     * @param code
     *            uno de {@code ANIMAL}, {@code OWNER}, {@code APPOINTMENT},
     *            {@code INVOICE}, {@code USER}, {@code BRANCH}, {@code TERMINAL} o
     *            {@code STORAGE_GB}
     */
    /**
     * El id de un articulo del catalogo comercial que ya siembra el changeset 308.
     *
     * <p>
     * <strong>No se siembra aqui, se resuelve</strong>, por las dos razones que
     * documenta la cabecera de esta clase. La primera es la colision: hasta #647
     * este andamio insertaba su propio {@code 'CORE'} con id fijo y la clausula
     * {@code ON DUPLICATE KEY} se tragaba el choque contra el indice unico de
     * {@code catalog_items.code}, dejando un id que ninguna clave foranea
     * alcanzaba. La segunda pesa mas: las consultas del minimo estructural buscan
     * literalmente {@code ci.code = 'CORE'}, asi que la unica fila sobre la que
     * tiene sentido afirmar es la que produccion mira.
     *
     * @param code
     *            uno de los 26 codigos del changeset 308 ({@code CORE},
     *            {@code SCHEDULING}, {@code CAPACITY_USER}, {@code EXTRA_STORAGE}…)
     */
    public static Long catalogItemId(EntityManager em, String code) {
        return ((Number) em.createNativeQuery("SELECT id FROM catalog_items WHERE code = :code")
                .setParameter("code", code).getSingleResult()).longValue();
    }

    public static Long limitDimensionId(EntityManager em, String code) {
        return ((Number) em.createNativeQuery("SELECT id FROM limit_dimensions WHERE code = :code")
                .setParameter("code", code).getSingleResult()).longValue();
    }

    /**
     * Inserta la fila si —y solo si— ese {@code id} no esta ya ocupado.
     *
     * <p>
     * <b>La guarda es por clave primaria, y esa precision es el arreglo entero.</b>
     * Lo que habia antes era {@code ON DUPLICATE KEY UPDATE id = id}, que perdona
     * <em>cualquier</em> indice unico: perdonaba re-sembrar la misma fila (util) y
     * perdonaba tambien chocar contra una fila ajena que ocupaba la misma clave de
     * negocio (catastrofico). Eso segundo es #647: el changeset 308 sembro
     * {@code catalog_items.code = 'CORE'}, el andamio choco contra
     * {@code uq_catalog_items_code}, la clausula lo convirtio en un no-op y el id
     * fijo no llego a existir nunca.
     *
     * <p>
     * Con esta guarda las dos cosas se separan. Re-sembrar un id que ya esta puesto
     * —lo hacen varias rodajas que siembran geografia por su cuenta antes de llamar
     * aqui, y el contenedor MySQL es uno solo para toda la suite— no es un error y
     * se salta en silencio. Todo lo demas revienta en el sitio y con el mensaje
     * correcto: clave foranea que no resuelve, {@code CHECK} incumplido, columna
     * {@code NOT NULL} omitida y —la que importa— colision contra una clave de
     * negocio ajena, porque esa fila tiene otro {@code id} y la guarda no la ve.
     *
     * <p>
     * Dicho de otro modo: el andamio sigue siendo idempotente, pero ya no puede
     * mentir sobre haber sembrado.
     */
    private static void insert(EntityManager em, String sql, Long id) {
        if (yaOcupado(em, tablaDe(sql), id))
            return;
        em.createNativeQuery(sql).setParameter("id", id).executeUpdate();
    }

    /**
     * El nombre de la tabla, leido del propio {@code INSERT}. Si algun dia una
     * sentencia deja de encajar, esto revienta aqui —con la sentencia delante— en
     * vez de degradar la guarda a un no-op, que seria repetir el error de #647 en
     * el mecanismo puesto para evitarlo.
     */
    private static String tablaDe(String sql) {
        String marca = "INSERT INTO ";
        int desde = sql.indexOf(marca);
        if (desde < 0)
            throw new IllegalArgumentException("No se pudo leer la tabla del INSERT: " + sql);
        desde += marca.length();
        int hasta = desde;
        while (hasta < sql.length()
                && (Character.isLetterOrDigit(sql.charAt(hasta)) || sql.charAt(hasta) == '_'))
            hasta++;
        if (hasta == desde)
            throw new IllegalArgumentException("No se pudo leer la tabla del INSERT: " + sql);
        return sql.substring(desde, hasta);
    }

    /**
     * La lectura va con {@code FOR SHARE} a proposito. El contenedor MySQL es uno
     * solo para toda la suite y el aislamiento por defecto es
     * {@code REPEATABLE READ}: una lectura normal responde contra la instantanea
     * con la que empezo la transaccion. Si otra rodaja sembro esta misma fila y
     * confirmo despues de ese instante, la lectura dice "libre" y el indice unico
     * dice "duplicado" un renglon mas abajo, que es justo el fallo que esta guarda
     * existe para evitar. Una lectura con bloqueo ve la ultima version confirmada.
     */
    private static boolean yaOcupado(EntityManager em, String tabla, Long id) {
        String sql = "SELECT 1 FROM " + tabla + " WHERE id = :id FOR SHARE";
        return !em.createNativeQuery(sql).setParameter("id", id).getResultList().isEmpty();
    }
}
