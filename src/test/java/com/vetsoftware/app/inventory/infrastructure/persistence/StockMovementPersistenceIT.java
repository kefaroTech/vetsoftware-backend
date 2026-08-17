package com.vetsoftware.app.inventory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.inventory.domain.StockMovement;
import com.vetsoftware.app.inventory.domain.StockMovementType;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del libro de movimientos contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve.</b> Lo que hay que verificar aqui no es codigo
 * Java, es lo que la base acepta y devuelve. Tres cosas concretas:
 *
 * <ul>
 * <li>El kardex es <b>append-only</b> y no tiene indice unico sobre
 * {@code (reference_type, reference_id, product_id)}: dos ventas identicas del
 * mismo producto tienen que dejar dos asientos. Un doble en memoria diria que
 * si sin haber mirado la tabla.</li>
 * <li>Los enums viajan como texto a columnas acotadas —{@code type} es
 * {@code VARCHAR(20)} y {@code reference_type} es {@code VARCHAR(30)}—, asi que
 * una constante nueva mas larga que el limite explota en produccion con <i>Data
 * too long</i> y en ningun test unitario. Por eso el recorrido de los dos enums
 * va contra la base.</li>
 * <li>{@code quantity} es un {@code INT} con signo y {@code unit_cost} un
 * {@code DECIMAL(19,4)}: el signo de una salida y los cuatro decimales del
 * costo sostienen el saldo y la valuacion, y solo el motor dice si sobreviven
 * al viaje.</li>
 * </ul>
 *
 * <p>
 * <b>Que NO se prueba aqui.</b> El kardex paginado, el orden temporal
 * descendente, el libro de compras y el saldo inicial no los sirve este
 * adaptador sino {@code StockQueryAdapter}, y ya tienen su rodaja en
 * {@code StockQueryAdapterIT}. Este test cubre el puerto de escritura
 * {@code StockMovementRepository}: guardar, la idempotencia por referencia y la
 * recuperacion para compensar.
 */
@Import(JpaStockMovementRepository.class)
@DisplayName("JpaStockMovementRepository — el libro de movimientos contra MySQL real")
class StockMovementPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;
    private static final Long OTRA_BRANCH = SchemaSeed.OTRA_BRANCH_ID;
    private static final Long PRODUCT = SchemaSeed.PRODUCT_ID;
    private static final Long OTRO_PRODUCT = SchemaSeed.OTRO_PRODUCT_ID;
    private static final Long EMPLOYEE = SchemaSeed.EMPLOYEE_ID;

    private static final BigDecimal COSTO = new BigDecimal("1500.0000");
    private static final LocalDateTime ENERO_10 = LocalDateTime.of(2026, 1, 10, 9, 0);
    private static final Long RECEPCION = 4001L;

    @Autowired
    private JpaStockMovementRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    /** Lote de la empresa propia: {@code lot_id} tiene FK real y es NOT NULL. */
    private Long lote;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        lote = crearLote(COMPANY, BRANCH, PRODUCT);
    }

    private Long crearLote(Long companyId, Long branchId, Long productId) {
        entityManager.createNativeQuery("""
                INSERT INTO stock_lot (company_id, branch_id, product_id, lot_number, expire_date,
                                       quantity_available, unit_cost, received_date, created_date,
                                       enabled)
                VALUES (?, ?, ?, 'L-1', NULL, 10, ?, ?, ?, true)
                """).setParameter(1, companyId).setParameter(2, branchId).setParameter(3, productId)
                .setParameter(4, COSTO).setParameter(5, ENERO_10).setParameter(6, ENERO_10)
                .executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()")
                .getSingleResult()).longValue();
    }

    private StockMovement asiento(Long companyId, Long branchId, Long productId, Long loteId,
            StockMovementType tipo, int cantidad, StockReferenceType refTipo, Long refId) {
        return repository.save(new StockMovement(null, companyId, branchId, productId, loteId, tipo,
                cantidad, COSTO, refTipo, refId, "prueba", EMPLOYEE, ENERO_10));
    }

    private StockMovement entrada(Long productId, Long refId) {
        return asiento(COMPANY, BRANCH, productId, lote, StockMovementType.PURCHASE, 5,
                StockReferenceType.GOODS_RECEIPT, refId);
    }

    /**
     * Fuerza el INSERT y vacia el contexto de persistencia: sin el {@code clear()}
     * la consulta devolveria la instancia que ya esta en memoria y el test no
     * probaria ni una columna.
     */
    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("save — que queda escrito en el asiento")
    class Escritura {

        @Test
        @DisplayName("guardar y releer conserva cada campo del asiento")
        void guardar_y_releer_conserva_cada_campo() {
            entrada(PRODUCT, RECEPCION);
            releerDesdeLaBase();

            StockMovement leido = repository.findByReferenceAndCompanyId(
                    StockReferenceType.GOODS_RECEIPT, RECEPCION, COMPANY).getFirst();

            assertThat(leido.getId()).isNotNull();
            assertThat(leido.getCompanyId()).isEqualTo(COMPANY);
            assertThat(leido.getBranchId()).isEqualTo(BRANCH);
            assertThat(leido.getProductId()).isEqualTo(PRODUCT);
            assertThat(leido.getLotId()).isEqualTo(lote);
            assertThat(leido.getType()).isEqualTo(StockMovementType.PURCHASE);
            assertThat(leido.getQuantity()).isEqualTo(5);
            assertThat(leido.getUnitCost()).isEqualByComparingTo(COSTO);
            assertThat(leido.getReferenceType()).isEqualTo(StockReferenceType.GOODS_RECEIPT);
            assertThat(leido.getReferenceId()).isEqualTo(RECEPCION);
            assertThat(leido.getReason()).isEqualTo("prueba");
            assertThat(leido.getCreatedBy()).isEqualTo(EMPLOYEE);
            assertThat(leido.getCreatedDate()).isEqualTo(ENERO_10);
        }

        @Test
        @DisplayName("el signo negativo de una salida sobrevive al viaje")
        void el_signo_negativo_de_una_salida_sobrevive() {
            // Si la columna fuese INT UNSIGNED, MySQL rechazaria o truncaria el negativo y
            // el saldo del kardex —que es la suma con signo— quedaria inflado sin que nada
            // fallara en Java.
            asiento(COMPANY, BRANCH, PRODUCT, lote, StockMovementType.SALE, -3,
                    StockReferenceType.POS_DOCUMENT, RECEPCION);
            releerDesdeLaBase();

            assertThat(repository.findByReferenceAndCompanyId(StockReferenceType.POS_DOCUMENT,
                    RECEPCION, COMPANY).getFirst().getQuantity()).isEqualTo(-3);
        }

        @Test
        @DisplayName("el costo unitario conserva sus cuatro decimales")
        void el_costo_conserva_sus_cuatro_decimales() {
            // DECIMAL(19,4): el costo se multiplica por miles de unidades en la valuacion,
            // asi que redondearlo a dos decimales mueve el inventario valorado.
            repository.save(new StockMovement(null, COMPANY, BRANCH, PRODUCT, lote,
                    StockMovementType.PURCHASE, 5, new BigDecimal("1234.5678"),
                    StockReferenceType.GOODS_RECEIPT, RECEPCION, null, EMPLOYEE, ENERO_10));
            releerDesdeLaBase();

            assertThat(repository.findByReferenceAndCompanyId(StockReferenceType.GOODS_RECEIPT,
                    RECEPCION, COMPANY).getFirst().getUnitCost()).isEqualByComparingTo("1234.5678");
        }

        @Test
        @DisplayName("dos asientos identicos dejan dos filas: el libro es append-only")
        void dos_asientos_identicos_dejan_dos_filas() {
            // El kardex no deduplica: dos ventas iguales del mismo producto en el mismo
            // documento son dos salidas reales. Un indice unico sobre
            // (reference_type, reference_id, product_id) convertiria la segunda en un error
            // silencioso y dejaria stock de mas. La idempotencia se decide antes, en
            // existsByReference, no con una restriccion de la tabla.
            entrada(PRODUCT, RECEPCION);
            entrada(PRODUCT, RECEPCION);
            releerDesdeLaBase();

            assertThat(repository.findByReferenceAndCompanyId(StockReferenceType.GOODS_RECEIPT,
                    RECEPCION, COMPANY)).hasSize(2);
        }

        @ParameterizedTest
        @EnumSource(StockMovementType.class)
        @DisplayName("cada tipo de movimiento cabe en la columna y vuelve como el mismo enum")
        void cada_tipo_de_movimiento_vuelve_igual(StockMovementType tipo) {
            // type es VARCHAR(20): una constante nueva mas larga se guarda truncada o
            // revienta con "Data too long", y el valueOf de vuelta lanzaria
            // IllegalArgumentException al releer el kardex.
            asiento(COMPANY, BRANCH, PRODUCT, lote, tipo, 5, StockReferenceType.GOODS_RECEIPT,
                    RECEPCION);
            releerDesdeLaBase();

            assertThat(repository.findByReferenceAndCompanyId(StockReferenceType.GOODS_RECEIPT,
                    RECEPCION, COMPANY).getFirst().getType()).isEqualTo(tipo);
        }

        @ParameterizedTest
        @EnumSource(StockReferenceType.class)
        @DisplayName("cada tipo de referencia cabe en la columna y vuelve como el mismo enum")
        void cada_tipo_de_referencia_vuelve_igual(StockReferenceType refTipo) {
            // reference_type es VARCHAR(30) y es la mitad de la clave de idempotencia:
            // truncarlo haria que dos origenes distintos parezcan el mismo.
            asiento(COMPANY, BRANCH, PRODUCT, lote, StockMovementType.PURCHASE, 5, refTipo,
                    RECEPCION);
            releerDesdeLaBase();

            assertThat(repository.findByReferenceAndCompanyId(refTipo, RECEPCION, COMPANY)
                    .getFirst().getReferenceType()).isEqualTo(refTipo);
        }
    }

    @Nested
    @DisplayName("existsByReference — la idempotencia de una referencia ya procesada")
    class Idempotencia {

        @Test
        @DisplayName("reconoce que la referencia ya movio ese producto")
        void reconoce_que_la_referencia_ya_movio_ese_producto() {
            entrada(PRODUCT, RECEPCION);
            releerDesdeLaBase();

            assertThat(repository.existsByReference(StockReferenceType.GOODS_RECEIPT, RECEPCION,
                    PRODUCT)).isTrue();
        }

        @Test
        @DisplayName("el producto es parte de la clave, no un detalle")
        void el_producto_es_parte_de_la_clave() {
            // Una venta de N productos emite N asientos bajo el mismo documento. Si la
            // clave fuera solo la referencia, la segunda linea y siguientes se
            // descartarian como duplicado y el stock quedaria sobrestimado en silencio.
            entrada(PRODUCT, RECEPCION);
            releerDesdeLaBase();

            assertThat(repository.existsByReference(StockReferenceType.GOODS_RECEIPT, RECEPCION,
                    OTRO_PRODUCT)).isFalse();
        }

        @Test
        @DisplayName("el mismo id bajo otro tipo de referencia no es duplicado")
        void el_mismo_id_bajo_otro_tipo_de_referencia_no_es_duplicado() {
            // Los ids son autoincrementales por tabla: la recepcion 4001 y el documento
            // POS 4001 existen a la vez y no tienen nada que ver.
            entrada(PRODUCT, RECEPCION);
            releerDesdeLaBase();

            assertThat(repository.existsByReference(StockReferenceType.POS_DOCUMENT, RECEPCION,
                    PRODUCT)).isFalse();
        }

        @Test
        @DisplayName("otra referencia del mismo tipo no es duplicado")
        void otra_referencia_del_mismo_tipo_no_es_duplicado() {
            entrada(PRODUCT, RECEPCION);
            releerDesdeLaBase();

            assertThat(
                    repository.existsByReference(StockReferenceType.GOODS_RECEIPT, 4002L, PRODUCT))
                    .isFalse();
        }

        @Test
        @DisplayName("sin referencia o sin producto responde que no, y no consulta la base")
        void sin_referencia_o_sin_producto_responde_que_no() {
            // La guarda de nulls evita que un `WHERE reference_id = NULL` —que en SQL no
            // iguala nada— se lea como "no hay duplicado" por accidente.
            assertThat(
                    repository.existsByReference(StockReferenceType.GOODS_RECEIPT, null, PRODUCT))
                    .isFalse();
            assertThat(
                    repository.existsByReference(StockReferenceType.GOODS_RECEIPT, RECEPCION, null))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("findByReferenceAndCompanyId — lo que se recupera para compensar")
    class Compensacion {

        @Test
        @DisplayName("trae todos los asientos de la referencia, uno por producto")
        void trae_todos_los_asientos_de_la_referencia() {
            // Anular una recepcion de dos productos tiene que compensar los dos asientos:
            // si la consulta devolviera solo el primero, el segundo quedaria en el stock
            // para siempre.
            entrada(PRODUCT, RECEPCION);
            entrada(OTRO_PRODUCT, RECEPCION);
            releerDesdeLaBase();

            assertThat(repository.findByReferenceAndCompanyId(StockReferenceType.GOODS_RECEIPT,
                    RECEPCION, COMPANY)).extracting(StockMovement::getProductId)
                    .containsExactlyInAnyOrder(PRODUCT, OTRO_PRODUCT);
        }

        @Test
        @DisplayName("no acota por sede: una transferencia se compensa en las dos puntas")
        void no_acota_por_sede() {
            // Una transferencia emite TRANSFER_OUT en origen y TRANSFER_IN en destino bajo
            // la misma referencia. Si la consulta filtrara por sede, revertirla desharia
            // solo la mitad y las dos sedes quedarian descuadradas.
            Long loteDestino = crearLote(COMPANY, OTRA_BRANCH, PRODUCT);
            asiento(COMPANY, BRANCH, PRODUCT, lote, StockMovementType.TRANSFER_OUT, -4,
                    StockReferenceType.TRANSFER, RECEPCION);
            asiento(COMPANY, OTRA_BRANCH, PRODUCT, loteDestino, StockMovementType.TRANSFER_IN, 4,
                    StockReferenceType.TRANSFER, RECEPCION);
            releerDesdeLaBase();

            assertThat(repository.findByReferenceAndCompanyId(StockReferenceType.TRANSFER,
                    RECEPCION, COMPANY)).extracting(StockMovement::getBranchId)
                    .containsExactlyInAnyOrder(BRANCH, OTRA_BRANCH);
        }

        @Test
        @DisplayName("los asientos de otra empresa no se recuperan")
        void los_asientos_de_otra_empresa_no_se_recuperan() {
            Long loteAjeno = crearLote(OTRA_COMPANY, BRANCH, PRODUCT);
            entrada(PRODUCT, RECEPCION);
            asiento(OTRA_COMPANY, BRANCH, PRODUCT, loteAjeno, StockMovementType.PURCHASE, 5,
                    StockReferenceType.GOODS_RECEIPT, RECEPCION);
            releerDesdeLaBase();

            List<StockMovement> propios = repository.findByReferenceAndCompanyId(
                    StockReferenceType.GOODS_RECEIPT, RECEPCION, COMPANY);

            // Compensar con los movimientos de otro tenant escribiria ajustes en un
            // inventario ajeno.
            assertThat(propios).singleElement().extracting(StockMovement::getCompanyId)
                    .isEqualTo(COMPANY);
        }

        @Test
        @DisplayName("una referencia sin asientos devuelve lista vacia, no null")
        void una_referencia_sin_asientos_devuelve_vacio() {
            assertThat(repository.findByReferenceAndCompanyId(StockReferenceType.GOODS_RECEIPT,
                    4002L, COMPANY)).isEmpty();
        }

        @Test
        @DisplayName("sin referencia devuelve lista vacia sin ir a la base")
        void sin_referencia_devuelve_lista_vacia() {
            assertThat(repository.findByReferenceAndCompanyId(StockReferenceType.GOODS_RECEIPT,
                    null, COMPANY)).isEmpty();
        }
    }
}
