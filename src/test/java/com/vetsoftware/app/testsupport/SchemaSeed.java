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
 * cuelgan de ciudad, membresia y categoria. La cadena completa es pais →
 * departamento → ciudad → membresia → empresa → sede → categoria → producto, y
 * sin ella cualquier {@code @DataJpaTest} de inventario, caja o cuenta abierta
 * muere con un {@code DataIntegrityViolation}.
 *
 * <p>
 * <b>Solo columnas obligatorias.</b> Se insertan por SQL nativo, no por
 * entidades JPA, para no arrastrar las invariantes de siete agregados ajenos
 * dentro de un test que solo quiere un lote. Los ids son fijos y explicitos
 * ({@link #COMPANY_ID} y compañia) para que las aserciones puedan nombrarlos.
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
    public static final Long MEMBERSHIP_ID = 900L;
    public static final Long COMPANY_ID = 900L;
    public static final Long OTRA_COMPANY_ID = 901L;
    public static final Long BRANCH_ID = 910L;
    public static final Long OTRA_BRANCH_ID = 911L;
    public static final Long CATEGORY_ID = 920L;
    public static final Long PRODUCT_ID = 930L;
    public static final Long OTRO_PRODUCT_ID = 931L;
    public static final Long EMPLOYEE_ID = 940L;

    private SchemaSeed() {
    }

    /** Inserta la cadena completa. Idempotente dentro de la misma transaccion. */
    public static void seed(EntityManager em) {
        insert(em, """
                INSERT IGNORE INTO countries (id, name) VALUES (:id, 'Colombia')
                """, COUNTRY_ID);
        insert(em, """
                INSERT IGNORE INTO states (id, name, country_id)
                VALUES (:id, 'Antioquia', %d)
                """.formatted(COUNTRY_ID), STATE_ID);
        insert(em, """
                INSERT IGNORE INTO cities (id, name, state_id) VALUES (:id, 'Medellin', %d)
                """.formatted(STATE_ID), CITY_ID);
        insert(em,
                """
                        INSERT IGNORE INTO memberships (id, name, status) VALUES (:id, 'Plan test', 'ACTIVE')
                        """,
                MEMBERSHIP_ID);

        company(em, COMPANY_ID, "Veterinaria de prueba", "900123456");
        company(em, OTRA_COMPANY_ID, "Veterinaria ajena", "900654321");
        branch(em, BRANCH_ID, "Sede Centro", "CENTRO", COMPANY_ID);
        branch(em, OTRA_BRANCH_ID, "Sede Norte", "NORTE", COMPANY_ID);

        insert(em, """
                INSERT IGNORE INTO product_categories (id, name, description, company_id)
                VALUES (:id, 'Medicamentos', 'Categoria de prueba', %d)
                """.formatted(COMPANY_ID), CATEGORY_ID);
        product(em, PRODUCT_ID, "Amoxicilina 500mg", "SKU-100");
        product(em, OTRO_PRODUCT_ID, "Ivermectina 1%", "SKU-200");

        insert(em, """
                INSERT IGNORE INTO employees (id, employee_code, hash_password, name, email,
                                              company_id)
                VALUES (:id, 'EMP-001', 'x', 'Ana Ruiz', 'ana@test.local', %d)
                """.formatted(COMPANY_ID), EMPLOYEE_ID);
        em.flush();
    }

    private static void company(EntityManager em, Long id, String nombre, String nit) {
        insert(em, """
                INSERT IGNORE INTO companies (id, name, identifier, city_id, membership_id)
                VALUES (:id, '%s', '%s', %d, %d)
                """.formatted(nombre, nit, CITY_ID, MEMBERSHIP_ID), id);
    }

    private static void branch(EntityManager em, Long id, String nombre, String codigo,
            Long companyId) {
        insert(em, """
                INSERT IGNORE INTO branches (id, name, code, city_id, company_id)
                VALUES (:id, '%s', '%s', %d, %d)
                """.formatted(nombre, codigo, CITY_ID, companyId), id);
    }

    private static void product(EntityManager em, Long id, String nombre, String codigo) {
        insert(em, """
                INSERT IGNORE INTO products (id, name, code, sale_price, product_category_id,
                                             company_id, tax_treatment)
                VALUES (:id, '%s', '%s', 10000.00, %d, %d, 'GRAVADO')
                """.formatted(nombre, codigo, CATEGORY_ID, COMPANY_ID), id);
    }

    private static void insert(EntityManager em, String sql, Long id) {
        em.createNativeQuery(sql).setParameter("id", id).executeUpdate();
    }
}
