package com.vetsoftware.app.openaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
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
 * Rodaja del calculo de totales de la cuenta abierta contra MySQL real.
 *
 * <p>
 * Este adaptador no tiene logica propia: suma cuatro agregados SQL que viven en
 * cuatro tablas de cuatro features distintas. Todo lo que puede fallar esta en
 * esas consultas —que el {@code enabled = true} vaya explicito porque el
 * {@code @SQLRestriction} no aplica a los agregados, que el {@code voided =
 * false} excluya lo anulado, que el {@code COALESCE} devuelva cero y no
 * {@code NULL} sobre cero filas, y que el {@code SUM} sobre {@code DECIMAL} no
 * pierda centavos—. Con dobles de los cuatro repositorios el test solo
 * comprobaria que 1 + 1 + 1 + 1 son 4.
 *
 * <p>
 * Las filas se siembran por SQL nativo a proposito: lo que se prueba es la
 * consulta contra la tabla, no el camino de escritura de cada feature hija (que
 * tiene su propia rodaja).
 */
@Import(JpaOpenAccountTotalsAdapter.class)
@DisplayName("JpaOpenAccountTotalsAdapter — la suma de cargos y abonos contra MySQL real")
class OpenAccountTotalsAdapterIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;
    private static final Long OTRA_BRANCH = SchemaSeed.OTRA_BRANCH_ID;
    private static final Long EMPLEADO = SchemaSeed.EMPLOYEE_ID;
    private static final Long PRODUCTO = SchemaSeed.PRODUCT_ID;

    private static final Long OWNER = 960L;
    private static final Long OTRO_OWNER = 961L;
    private static final Long CUENTA = 970L;
    private static final Long OTRA_CUENTA = 971L;
    private static final Long CUENTA_VACIA = 972L;
    private static final Long ANIMAL = 973L;
    private static final Long SPECIE = 974L;
    private static final Long BREED = 975L;
    private static final Long COLOR = 976L;
    private static final Long SERVICIO = 977L;
    private static final Long CATEGORIA_SERVICIO = 978L;

    @Autowired
    private JpaOpenAccountTotalsAdapter adapter;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        propietario(OWNER, "Marta Diaz", "CC-1001");
        propietario(OTRO_OWNER, "Jorge Lima", "CC-1002");
        catalogoDeAnimal();
        catalogoDeServicio();
        cuenta(CUENTA, OWNER, BRANCH);
        cuenta(OTRA_CUENTA, OTRO_OWNER, BRANCH);
        cuenta(CUENTA_VACIA, OTRO_OWNER, OTRA_BRANCH);
        entityManager.flush();

        // Guardia de la siembra. Toda la cadena de arriba va con INSERT IGNORE, y MySQL
        // degrada a warning tanto el NOT NULL sin valor como la violacion de FK: la
        // fila
        // simplemente no se inserta y nadie se entera. Sin esta comprobacion el sintoma
        // aparece tres tablas mas abajo —una FK rota al insertar el cargo— y apunta a
        // la
        // tabla equivocada. Aqui falla donde de verdad falta el dato.
        assertThat(filas("animals", ANIMAL)).as("el animal al que se cuelgan los cargos").isOne();
        assertThat(filas("services", SERVICIO)).as("el servicio de los cargos").isOne();
    }

    private long filas(String tabla, Long id) {
        Number total = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM " + tabla + " WHERE id = :id")
                .setParameter("id", id).getSingleResult();
        return total.longValue();
    }

    private void propietario(Long id, String nombre, String documento) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO owners (id, name, document, document_type, person_type,
                                           withholding_agent, tax_regime, fiscal_responsibility,
                                           city_id, company_id, created_date, enabled)
                VALUES (:id, :nombre, :documento, 'CEDULA_CIUDADANIA', 'NATURAL', false,
                        'NO_RESPONSABLE_IVA', 'NO_APLICA', :ciudad, :empresa,
                        '2026-01-01 08:00:00', true)
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("documento", documento).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", COMPANY).executeUpdate();
    }

    private void catalogoDeAnimal() {
        ejecutar("""
                INSERT IGNORE INTO species (id, name, created_date, enabled)
                VALUES (:id, 'Canino', '2026-01-01 08:00:00', true)
                """, SPECIE);
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO breeds (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Criollo', :specie, '2026-01-01 08:00:00', true)
                """).setParameter("id", BREED).setParameter("specie", SPECIE).executeUpdate();
        // El color NO es un catalogo global: la migracion 218 le colgo un specie_id NOT
        // NULL
        // con FK a species. Sin esa columna el INSERT IGNORE se traga el fallo en
        // silencio, el
        // color no existe, la FK fk_animals_color tumba al animal —tambien en silencio—
        // y el
        // error solo aparece tres tablas mas abajo, al insertar el cargo de producto.
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animal_colors (id, name, specie_id, created_date, enabled)
                VALUES (:id, 'Negro', :specie, '2026-01-01 08:00:00', true)
                """).setParameter("id", COLOR).setParameter("specie", SPECIE).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO animals (id, name, code, specie_id, breed_id, owner_id, gender,
                                            weight_type, animal_type, reproductive_state, color_id,
                                            deceased, company_id, created_date, enabled)
                VALUES (:id, 'Firulais', 'A-001', :specie, :breed, :owner, 'MALE', 'KILOGRAMS',
                        'NONE', 'UNKNOWN', :color, false, :empresa, '2026-01-01 08:00:00', true)
                """).setParameter("id", ANIMAL).setParameter("specie", SPECIE)
                .setParameter("breed", BREED).setParameter("owner", OWNER)
                .setParameter("color", COLOR).setParameter("empresa", COMPANY).executeUpdate();
    }

    private void catalogoDeServicio() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO service_categories (id, name, description, company_id,
                                                       created_date, enabled)
                VALUES (:id, 'Consulta', 'Categoria de prueba', :empresa, '2026-01-01 08:00:00',
                        true)
                """).setParameter("id", CATEGORIA_SERVICIO).setParameter("empresa", COMPANY)
                .executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO services (id, name, price, service_category_id, company_id,
                                             tax_treatment, created_date, enabled)
                VALUES (:id, 'Consulta general', 40000.00, :categoria, :empresa, 'EXCLUIDO',
                        '2026-01-01 08:00:00', true)
                """).setParameter("id", SERVICIO).setParameter("categoria", CATEGORIA_SERVICIO)
                .setParameter("empresa", COMPANY).executeUpdate();
    }

    private void cuenta(Long id, Long ownerId, Long branchId) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO open_accounts (id, total_amount, paid_amount,
                                                  outstanding_amount, owner_id, company_id,
                                                  branch_id, created_by_id, created_date, enabled,
                                                  status, reversed, version)
                VALUES (:id, 0.00, 0.00, 0.00, :owner, :empresa, :sede, :empleado,
                        '2026-01-15 09:00:00', true, 'OPEN', false, 0)
                """).setParameter("id", id).setParameter("owner", ownerId)
                .setParameter("empresa", COMPANY).setParameter("sede", branchId)
                .setParameter("empleado", EMPLEADO).executeUpdate();
    }

    private void ejecutar(String sql, Long id) {
        entityManager.createNativeQuery(sql).setParameter("id", id).executeUpdate();
    }

    private void cargoDeProducto(Long cuentaId, String total, boolean anulado, boolean habilitado) {
        entityManager.createNativeQuery("""
                INSERT INTO product_charge_open_accounts (animal_id, product_id, unit_price,
                                                          quantity, has_tax, base_amount,
                                                          tax_amount, total_amount,
                                                          open_account_id, created_by_id,
                                                          created_date, enabled, voided)
                VALUES (:animal, :producto, :total, 1, false, :total, 0.00, :total, :cuenta,
                        :empleado, '2026-01-15 10:00:00', :habilitado, :anulado)
                """).setParameter("animal", ANIMAL).setParameter("producto", PRODUCTO)
                .setParameter("total", new BigDecimal(total)).setParameter("cuenta", cuentaId)
                .setParameter("empleado", EMPLEADO).setParameter("habilitado", habilitado)
                .setParameter("anulado", anulado).executeUpdate();
    }

    private void cargoDeServicio(Long cuentaId, String total, boolean anulado) {
        entityManager.createNativeQuery("""
                INSERT INTO service_charge_open_accounts (animal_id, service_id, unit_price,
                                                          has_tax, base_amount, tax_amount,
                                                          total_amount, open_account_id,
                                                          created_by_id, created_date, enabled,
                                                          voided)
                VALUES (:animal, :servicio, :total, false, :total, 0.00, :total, :cuenta,
                        :empleado, '2026-01-15 10:05:00', true, :anulado)
                """).setParameter("animal", ANIMAL).setParameter("servicio", SERVICIO)
                .setParameter("total", new BigDecimal(total)).setParameter("cuenta", cuentaId)
                .setParameter("empleado", EMPLEADO).setParameter("anulado", anulado)
                .executeUpdate();
    }

    private void cargoGeneral(Long cuentaId, String total, boolean anulado) {
        entityManager.createNativeQuery("""
                INSERT INTO general_charge_open_accounts (name, unit_amount, quantity, has_tax,
                                                          base_amount, tax_amount, total_amount,
                                                          open_account_id, created_by_id,
                                                          created_date, enabled, voided)
                VALUES ('Hospedaje', :total, 1.00, false, :total, 0.00, :total, :cuenta, :empleado,
                        '2026-01-15 10:10:00', true, :anulado)
                """).setParameter("total", new BigDecimal(total)).setParameter("cuenta", cuentaId)
                .setParameter("empleado", EMPLEADO).setParameter("anulado", anulado)
                .executeUpdate();
    }

    private void abono(Long cuentaId, String importe, boolean anulado) {
        entityManager.createNativeQuery("""
                INSERT INTO debt_open_accounts (amount, payment_method, open_account_id,
                                                created_by_id, created_date, enabled, voided)
                VALUES (:importe, 'CASH', :cuenta, :empleado, '2026-01-15 11:00:00', true, :anulado)
                """).setParameter("importe", new BigDecimal(importe))
                .setParameter("cuenta", cuentaId).setParameter("empleado", EMPLEADO)
                .setParameter("anulado", anulado).executeUpdate();
    }

    @Nested
    @DisplayName("total de cargos: las tres tablas suman juntas")
    class Cargos {

        @Test
        @DisplayName("suma productos, servicios y cargos generales de la misma cuenta")
        void suma_las_tres_familias_de_cargo() {
            cargoDeProducto(CUENTA, "10000.00", false, true);
            cargoDeProducto(CUENTA, "5000.00", false, true);
            cargoDeServicio(CUENTA, "40000.00", false);
            cargoGeneral(CUENTA, "3000.00", false);

            assertThat(adapter.totalCharges(CUENTA)).isEqualByComparingTo("58000.00");
        }

        @Test
        @DisplayName("un cargo anulado deja de contar")
        void un_cargo_anulado_deja_de_contar() {
            cargoDeProducto(CUENTA, "10000.00", false, true);
            cargoDeProducto(CUENTA, "99999.00", true, true);
            cargoDeServicio(CUENTA, "40000.00", true);
            cargoGeneral(CUENTA, "3000.00", true);

            // Anular no borra la fila (sigue visible en el detalle con su motivo): lo que
            // cambia es que el total de la cuenta deja de incluirla. Si el `voided = false`
            // se cayera de la query, el cliente pagaria el cargo que se le anulo.
            assertThat(adapter.totalCharges(CUENTA)).isEqualByComparingTo("10000.00");
        }

        @Test
        @DisplayName("un cargo dado de baja tampoco cuenta: el filtro va explicito en el agregado")
        void un_cargo_dado_de_baja_tampoco_cuenta() {
            cargoDeProducto(CUENTA, "10000.00", false, true);
            cargoDeProducto(CUENTA, "77777.00", false, false);

            // El @SQLRestriction de la entidad NO se aplica a las consultas de agregado,
            // por eso el `enabled = true` esta escrito a mano en el JPQL. Es justo el tipo
            // de detalle que un doble no puede desmentir.
            assertThat(adapter.totalCharges(CUENTA)).isEqualByComparingTo("10000.00");
        }

        @Test
        @DisplayName("los cargos de otra cuenta no se cuelan")
        void los_cargos_de_otra_cuenta_no_se_cuelan() {
            cargoDeProducto(CUENTA, "10000.00", false, true);
            cargoDeProducto(OTRA_CUENTA, "50000.00", false, true);
            cargoDeServicio(OTRA_CUENTA, "40000.00", false);

            assertThat(adapter.totalCharges(CUENTA)).isEqualByComparingTo("10000.00");
            assertThat(adapter.totalCharges(OTRA_CUENTA)).isEqualByComparingTo("90000.00");
        }

        @Test
        @DisplayName("una cuenta sin cargos suma cero, no null")
        void una_cuenta_sin_cargos_suma_cero() {
            // SUM sobre cero filas es NULL en SQL. Aqui lo tapan dos capas —el COALESCE de
            // cada query y el nullToZero del adaptador—, y el recalculo de la cuenta
            // explotaria con un NPE si ninguna lo hiciera.
            assertThat(adapter.totalCharges(CUENTA_VACIA)).isEqualByComparingTo("0");
            assertThat(adapter.totalPayments(CUENTA_VACIA)).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("los centavos sobreviven a la suma de tres cargos")
        void los_centavos_sobreviven_a_la_suma() {
            cargoDeProducto(CUENTA, "33.33", false, true);
            cargoDeServicio(CUENTA, "33.33", false);
            cargoGeneral(CUENTA, "33.33", false);

            // DECIMAL(12,2) sumado en la base: exacto. El mismo calculo en double daria
            // 99.98999999999999 y el saldo de la cuenta nunca llegaria a cero.
            assertThat(adapter.totalCharges(CUENTA)).isEqualByComparingTo("99.99");
        }
    }

    @Nested
    @DisplayName("total de abonos")
    class Abonos {

        @Test
        @DisplayName("suma los abonos vigentes de la cuenta")
        void suma_los_abonos_vigentes() {
            abono(CUENTA, "2000.00", false);
            abono(CUENTA, "1000.50", false);

            assertThat(adapter.totalPayments(CUENTA)).isEqualByComparingTo("3000.50");
        }

        @Test
        @DisplayName("un abono anulado deja de contar como pagado")
        void un_abono_anulado_deja_de_contar() {
            abono(CUENTA, "2000.00", false);
            abono(CUENTA, "5000.00", true);

            // Un abono anulado que siguiera sumando dejaria la cuenta como cobrada sin que
            // entrara el dinero: es la direccion en la que el error cuesta plata.
            assertThat(adapter.totalPayments(CUENTA)).isEqualByComparingTo("2000.00");
        }

        @Test
        @DisplayName("los abonos de otra cuenta no se cuelan")
        void los_abonos_de_otra_cuenta_no_se_cuelan() {
            abono(CUENTA, "2000.00", false);
            abono(OTRA_CUENTA, "9000.00", false);

            assertThat(adapter.totalPayments(CUENTA)).isEqualByComparingTo("2000.00");
        }

        @Test
        @DisplayName("los cargos no se mezclan con los abonos")
        void los_cargos_no_se_mezclan_con_los_abonos() {
            cargoDeProducto(CUENTA, "10000.00", false, true);
            abono(CUENTA, "2000.00", false);

            assertThat(adapter.totalCharges(CUENTA)).isEqualByComparingTo("10000.00");
            assertThat(adapter.totalPayments(CUENTA)).isEqualByComparingTo("2000.00");
        }
    }
}
