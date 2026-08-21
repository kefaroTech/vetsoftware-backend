package com.vetsoftware.app.inventory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.inventory.domain.StockLot;
import com.vetsoftware.app.inventory.domain.StockMovement;
import com.vetsoftware.app.inventory.domain.StockMovementType;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * Rodaja de persistencia de los lotes contra MySQL real.
 *
 * <p>
 * Estas dos consultas no se pueden probar con un doble sin que el test acabe
 * definiendo el contrato que dice verificar: el orden FEFO depende de como
 * ordena el motor los nulos, y la busqueda por identidad depende de como
 * compara MySQL un {@code NULL} contra un parametro. Justo el tipo de detalle
 * donde vivio BE-01.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaStockLotRepository — FEFO e identidad de lote contra MySQL real")
class StockLotPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;
    private static final Long OTRA_BRANCH = SchemaSeed.OTRA_BRANCH_ID;
    private static final Long PRODUCT = SchemaSeed.PRODUCT_ID;
    private static final BigDecimal COSTO = new BigDecimal("1500.0000");

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Autowired
    private JpaStockLotRepository repository;

    @Autowired
    private JpaStockMovementRepository movementRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private StockLot lote(String numero, LocalDate vence, int disponible) {
        return lote(numero, vence, disponible, COSTO, BRANCH, true);
    }

    private StockLot lote(String numero, LocalDate vence, int disponible, BigDecimal costo,
            Long branchId, boolean habilitado) {
        return repository.save(new StockLot(null, COMPANY, branchId, PRODUCT, numero, vence,
                disponible, costo, AHORA, AHORA, null, habilitado));
    }

    /**
     * Vacia el contexto de persistencia para que la siguiente lectura venga del
     * motor y no de la cache de primer nivel. Sin esto, media rodaja de bloqueo
     * optimista se responderia sola sin llegar a MySQL.
     */
    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    /** Cuenta las filas por SQL nativo, por fuera de Hibernate y de su cache. */
    private long filasDeLoteEnLaBase() {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM stock_lot WHERE product_id = :id")
                .setParameter("id", PRODUCT).getSingleResult()).longValue();
    }

    /**
     * El asiento de kardex que {@code consumeFefo} intercala entre sus dos
     * {@code save} del lote. No es decorado: es un INSERT real en medio de la
     * transaccion, y es lo que forzaria un flush intermedio —y con el, un conflicto
     * optimista falso— el dia que deje de ser un {@code persist} con id IDENTITY.
     */
    private void asentarEnElKardex(StockLot lote, int cantidad) {
        movementRepository.save(new StockMovement(null, COMPANY, BRANCH, PRODUCT, lote.getId(),
                StockMovementType.SALE, -cantidad, lote.getUnitCost(),
                StockReferenceType.POS_DOCUMENT, 7001L, "prueba", SchemaSeed.EMPLOYEE_ID, AHORA));
    }

    @Nested
    @DisplayName("findAvailableFefo — a que lote se le saca primero")
    class Fefo {

        @Test
        @DisplayName("saca primero el que vence antes")
        void saca_primero_el_que_vence_antes() {
            StockLot marzo = lote("L-MARZO", LocalDate.of(2027, 3, 31), 5);
            StockLot enero = lote("L-ENERO", LocalDate.of(2027, 1, 31), 5);
            StockLot junio = lote("L-JUNIO", LocalDate.of(2027, 6, 30), 5);

            List<StockLot> fefo = repository.findAvailableFefo(PRODUCT, BRANCH);

            assertThat(fefo).extracting(StockLot::getId).containsExactly(enero.getId(),
                    marzo.getId(), junio.getId());
        }

        @Test
        @DisplayName("los lotes sin vencimiento van al final, no al principio")
        void los_lotes_sin_vencimiento_van_al_final() {
            StockLot sinVencimiento = lote(null, null, 5);
            StockLot vence = lote("L-ENERO", LocalDate.of(2027, 1, 31), 5);

            List<StockLot> fefo = repository.findAvailableFefo(PRODUCT, BRANCH);

            // En MySQL los NULL ordenan primero por defecto: si el CASE WHEN del ORDER BY
            // se cayera, se consumiria antes el lote que no vence y se dejarian caducar
            // los que si. El bug no se ve hasta que hay merma.
            assertThat(fefo).extracting(StockLot::getId).containsExactly(vence.getId(),
                    sinVencimiento.getId());
        }

        @Test
        @DisplayName("con el mismo vencimiento desempata el lote mas antiguo")
        void con_el_mismo_vencimiento_desempata_el_mas_antiguo() {
            StockLot primero = lote("L-A", LocalDate.of(2027, 1, 31), 5);
            StockLot segundo = lote("L-B", LocalDate.of(2027, 1, 31), 5);

            assertThat(repository.findAvailableFefo(PRODUCT, BRANCH)).extracting(StockLot::getId)
                    .containsExactly(primero.getId(), segundo.getId());
        }

        @Test
        @DisplayName("un lote agotado no se ofrece")
        void un_lote_agotado_no_se_ofrece() {
            lote("L-VACIO", LocalDate.of(2027, 1, 31), 0);
            StockLot conStock = lote("L-CON-STOCK", LocalDate.of(2027, 6, 30), 5);

            assertThat(repository.findAvailableFefo(PRODUCT, BRANCH)).extracting(StockLot::getId)
                    .containsExactly(conStock.getId());
        }

        @Test
        @DisplayName("un lote en negativo tampoco se ofrece")
        void un_lote_en_negativo_tampoco_se_ofrece() {
            // Si la empresa permite stock negativo, el lote puede quedar bajo cero. No
            // puede volver a salir como disponible: se sacaria de un lote que no tiene.
            lote("L-NEGATIVO", LocalDate.of(2027, 1, 31), -3);

            assertThat(repository.findAvailableFefo(PRODUCT, BRANCH)).isEmpty();
        }

        @Test
        @DisplayName("un lote deshabilitado no se ofrece")
        void un_lote_deshabilitado_no_se_ofrece() {
            lote("L-BAJA", LocalDate.of(2027, 1, 31), 5, COSTO, BRANCH, false);

            assertThat(repository.findAvailableFefo(PRODUCT, BRANCH)).isEmpty();
        }

        @Test
        @DisplayName("los lotes de otra sede no se mezclan")
        void los_lotes_de_otra_sede_no_se_mezclan() {
            lote("L-OTRA-SEDE", LocalDate.of(2027, 1, 31), 5, COSTO, OTRA_BRANCH, true);
            StockLot propio = lote("L-PROPIO", LocalDate.of(2027, 6, 30), 5);

            // El stock es por sede: consumir de otra sede descuadraria las dos.
            assertThat(repository.findAvailableFefo(PRODUCT, BRANCH)).extracting(StockLot::getId)
                    .containsExactly(propio.getId());
        }
    }

    @Nested
    @DisplayName("findByIdentity — cuando una entrada acumula en el lote que ya existe")
    class Identidad {

        @Test
        @DisplayName("encuentra el lote con el mismo numero, vencimiento y costo")
        void encuentra_el_lote_con_la_misma_identidad() {
            StockLot existente = lote("L-2026-01", LocalDate.of(2027, 1, 31), 5);

            Optional<StockLot> encontrado = repository.findByIdentity(COMPANY, BRANCH, PRODUCT,
                    "L-2026-01", LocalDate.of(2027, 1, 31), COSTO);

            assertThat(encontrado).map(StockLot::getId).contains(existente.getId());
        }

        @Test
        @DisplayName("un lote generico se reconoce por sus nulls")
        void un_lote_generico_se_reconoce_por_sus_nulls() {
            StockLot generico = lote(null, null, 5);

            // En SQL `NULL = NULL` es NULL, no true: sin el `(:param IS NULL AND col IS
            // NULL)` del WHERE, cada entrada de un producto sin lote crearia una fila
            // nueva y el kardex acabaria con cientos de lotes de una unidad.
            Optional<StockLot> encontrado = repository.findByIdentity(COMPANY, BRANCH, PRODUCT,
                    null, null, COSTO);

            assertThat(encontrado).map(StockLot::getId).contains(generico.getId());
        }

        @Test
        @DisplayName("un costo distinto es otro lote: el costo real no se promedia")
        void un_costo_distinto_es_otro_lote() {
            lote("L-2026-01", LocalDate.of(2027, 1, 31), 5);

            Optional<StockLot> encontrado = repository.findByIdentity(COMPANY, BRANCH, PRODUCT,
                    "L-2026-01", LocalDate.of(2027, 1, 31), new BigDecimal("1800.0000"));

            // Acumular dos costos en el mismo lote perderia el costo real de entrada, que
            // es lo que sostiene la valuacion y el margen.
            assertThat(encontrado).isEmpty();
        }

        @Test
        @DisplayName("un vencimiento distinto es otro lote")
        void un_vencimiento_distinto_es_otro_lote() {
            lote("L-2026-01", LocalDate.of(2027, 1, 31), 5);

            assertThat(repository.findByIdentity(COMPANY, BRANCH, PRODUCT, "L-2026-01",
                    LocalDate.of(2027, 6, 30), COSTO)).isEmpty();
        }

        @Test
        @DisplayName("un lote con numero no lo encuentra la busqueda de generico")
        void un_lote_con_numero_no_lo_encuentra_la_busqueda_de_generico() {
            lote("L-2026-01", null, 5);

            assertThat(repository.findByIdentity(COMPANY, BRANCH, PRODUCT, null, null, COSTO))
                    .isEmpty();
        }

        @Test
        @DisplayName("un lote deshabilitado no se reutiliza")
        void un_lote_deshabilitado_no_se_reutiliza() {
            lote("L-2026-01", LocalDate.of(2027, 1, 31), 5, COSTO, BRANCH, false);

            assertThat(repository.findByIdentity(COMPANY, BRANCH, PRODUCT, "L-2026-01",
                    LocalDate.of(2027, 1, 31), COSTO)).isEmpty();
        }

        @Test
        @DisplayName("un lote agotado si se reutiliza: la entrada lo vuelve a llenar")
        void un_lote_agotado_si_se_reutiliza() {
            StockLot agotado = lote("L-2026-01", LocalDate.of(2027, 1, 31), 0);

            // A diferencia del FEFO, aqui el disponible no filtra: recibir mas unidades
            // del mismo lote tiene que sumar sobre la fila que ya existe.
            assertThat(repository.findByIdentity(COMPANY, BRANCH, PRODUCT, "L-2026-01",
                    LocalDate.of(2027, 1, 31), COSTO)).map(StockLot::getId)
                    .contains(agotado.getId());
        }
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva cada campo, incluidos los opcionales")
        void guardar_y_releer_conserva_cada_campo() {
            StockLot guardado = lote("L-2026-01", LocalDate.of(2027, 1, 31), 7);

            StockLot leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getCompanyId()).isEqualTo(COMPANY);
            assertThat(leido.getBranchId()).isEqualTo(BRANCH);
            assertThat(leido.getProductId()).isEqualTo(PRODUCT);
            assertThat(leido.getLotNumber()).isEqualTo("L-2026-01");
            assertThat(leido.getExpireDate()).isEqualTo(LocalDate.of(2027, 1, 31));
            assertThat(leido.getQuantityAvailable()).isEqualTo(7);
            assertThat(leido.getUnitCost()).isEqualByComparingTo(COSTO);
            assertThat(leido.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("el costo conserva sus cuatro decimales")
        void el_costo_conserva_sus_cuatro_decimales() {
            // La columna es DECIMAL(19,4): un costo unitario de fraccion se usa para
            // valorar miles de unidades, y redondearlo a dos decimales mueve el margen.
            StockLot guardado = lote("L-DECIMAL", null, 1, new BigDecimal("1234.5678"), BRANCH,
                    true);

            assertThat(repository.findById(guardado.getId()).orElseThrow().getUnitCost())
                    .isEqualByComparingTo("1234.5678");
        }
    }

    /**
     * BE-26 — el bloqueo optimista de {@code stock_lot} contra el motor.
     *
     * <p>
     * <b>Que camino protege el {@code @Version} aqui.</b> La hipotesis de partida
     * —dos ventas simultaneas del mismo lote se pisan— es <b>falsa</b>: todas las
     * salidas ({@code recordSale}, {@code recordClinicalUse},
     * {@code recordAdjustment}, {@code transfer}, {@code reverse}) pasan antes por
     * {@code balanceForUpdate}, que es un {@code SELECT ... FOR UPDATE} sobre
     * {@code stock_balance} y ya serializa por (producto, sede) —exactamente el
     * ambito de un lote—. El hueco real esta en {@code recordPurchase}:
     * {@code findByIdentity} → {@code lot.add(qty)} → {@code save(lot)} ocurren
     * <b>antes</b> de tomar ese lock, asi que una compra intercalada con una salida
     * del mismo lote es la unica operacion que puede pisar el descuento. Ese es el
     * escenario de {@link CompraQueLlegaTarde}.
     *
     * <p>
     * <b>Hasta donde llega esta rodaja — y hasta donde no.</b> Un
     * {@code @DataJpaTest} corre en UNA transaccion con rollback: montar dos
     * transacciones que se vean entre si exigiria commitear filas en el contenedor
     * que comparte toda la suite. Lo que si se reproduce exacto es el mecanismo que
     * decide el resultado, porque {@code JpaStockLotRepository} nunca trabaja con
     * entidades gestionadas: reconstruye una <b>desligada</b> desde el objeto de
     * dominio en cada {@code save}, asi que la version que llega al {@code merge}
     * es la que ese objeto leyo, y esa es la unica pieza que separa el conflicto
     * del pisoton silencioso. Lo que NO cubre esta rodaja es el entrelazado real de
     * dos conexiones ni el orden de sus commits: eso lo fija el motor, no el mapeo.
     */
    @Nested
    @DisplayName("bloqueo optimista de stock_lot (BE-26)")
    class BloqueoOptimista {

        @Test
        @DisplayName("un lote recien insertado nace con version 0")
        void un_lote_recien_insertado_nace_con_version_cero() {
            StockLot guardado = lote("L-2026-01", LocalDate.of(2027, 1, 31), 10);
            releerDesdeLaBase();

            assertThat(repository.findById(guardado.getId())).map(StockLot::getVersion)
                    .contains(0L);
        }

        @Test
        @DisplayName("guardar una copia obsoleta del lote sobre una fila ya modificada lanza el conflicto optimista")
        void guardar_una_copia_obsoleta_del_lote_lanza_el_conflicto_optimista() {
            Long id = lote("L-2026-01", LocalDate.of(2027, 1, 31), 10).getId();
            releerDesdeLaBase();

            StockLot copiaQueGana = repository.findById(id).orElseThrow();
            StockLot copiaQueQuedaraObsoleta = repository.findById(id).orElseThrow();
            assertThat(copiaQueQuedaraObsoleta.getVersion()).isZero();

            copiaQueGana.add(4);
            repository.save(copiaQueGana);
            releerDesdeLaBase();

            copiaQueQuedaraObsoleta.consume(2);

            assertThatThrownBy(() -> {
                repository.save(copiaQueQuedaraObsoleta);
                entityManager.flush();
            }).isInstanceOf(ObjectOptimisticLockingFailureException.class)
                    .hasMessageContaining("StockLotJpaEntity");
        }

        /**
         * {@code stock_lot} no tiene un {@code *JpaMapper}: el mapeo son dos
         * {@code toJpa}/{@code toDomain} privados dentro del propio adaptador, y el
         * {@code save} entrega una entidad <b>desligada</b>. Si la version no viajara
         * de vuelta por dominio y mapeo, llegaria {@code null} al {@code merge},
         * Hibernate concluiria que la fila es nueva e insertaria un duplicado en vez de
         * actualizar. El {@code isOne()} es quien lo delata.
         */
        @Test
        @DisplayName("guardar un lote ya existente actualiza la fila y sube la version, no inserta otra")
        void guardar_un_lote_existente_actualiza_y_no_inserta_una_segunda_fila() {
            Long id = lote("L-2026-01", LocalDate.of(2027, 1, 31), 10).getId();
            releerDesdeLaBase();

            StockLot cargado = repository.findById(id).orElseThrow();
            cargado.add(5);
            repository.save(cargado);
            releerDesdeLaBase();

            assertThat(filasDeLoteEnLaBase()).isOne();
            StockLot releido = repository.findById(id).orElseThrow();
            assertThat(releido.getQuantityAvailable()).isEqualTo(15);
            assertThat(releido.getVersion()).isEqualTo(1L);
        }
    }

    /**
     * El camino que motiva el {@code @Version} de {@code stock_lot}: la compra que
     * cargo el lote antes de que otra operacion lo moviera y llega tarde a
     * guardarlo.
     *
     * <p>
     * Los tres hechos se leen en orden dentro de cada caso: <b>quien cargo
     * primero</b> (la compra, por {@code findByIdentity}), <b>quien guardo
     * primero</b> (la salida, que ademas es la que si esta protegida por el lock
     * pesimista del saldo) y <b>quien llega tarde</b> (la compra, con su copia de
     * version obsoleta). Sin {@code @Version} el ultimo {@code UPDATE} escribiria
     * la suma calculada sobre la cantidad vieja y el descuento desapareceria sin un
     * solo error.
     */
    @Nested
    @DisplayName("recordPurchase — la compra que llega tarde no pisa el descuento (BE-26)")
    class CompraQueLlegaTarde {

        @Test
        @DisplayName("la compra que cargo el lote antes de la salida choca al guardar en vez de pisar la cantidad")
        void la_compra_que_llega_tarde_choca_en_vez_de_pisar_la_cantidad() {
            Long id = lote("L-2026-01", LocalDate.of(2027, 1, 31), 10).getId();
            releerDesdeLaBase();

            // 1. Carga primero la COMPRA: recordPurchase resuelve el lote por identidad y
            // se queda con una copia de version 0 y 10 unidades. Todavia no ha escrito
            // nada, y el lock del saldo lo tomara DESPUES de guardar el lote.
            StockLot loteQueVeLaCompra = repository.findByIdentity(COMPANY, BRANCH, PRODUCT,
                    "L-2026-01", LocalDate.of(2027, 1, 31), COSTO).orElseThrow();
            assertThat(loteQueVeLaCompra.getVersion()).isZero();
            assertThat(loteQueVeLaCompra.getQuantityAvailable()).isEqualTo(10);

            // 2. Guarda primero la SALIDA: descuenta 4 sobre el mismo lote y confirma.
            StockLot loteQueVeLaVenta = repository.findById(id).orElseThrow();
            loteQueVeLaVenta.consume(4);
            repository.save(loteQueVeLaVenta);
            releerDesdeLaBase();

            StockLot enLaBase = repository.findById(id).orElseThrow();
            assertThat(enLaBase.getQuantityAvailable()).isEqualTo(6);
            assertThat(enLaBase.getVersion()).isEqualTo(1L);

            // 3. Llega tarde la COMPRA: suma sus 5 sobre las 10 que leyo, no sobre las 6
            // que hay. Sin @Version el UPDATE dejaria 15 en la fila y las 4 unidades
            // vendidas se habrian esfumado del inventario sin ruido.
            loteQueVeLaCompra.add(5);
            assertThat(loteQueVeLaCompra.getQuantityAvailable()).isEqualTo(15);

            assertThatThrownBy(() -> {
                repository.save(loteQueVeLaCompra);
                entityManager.flush();
            }).isInstanceOf(ObjectOptimisticLockingFailureException.class)
                    .hasMessageContaining("StockLotJpaEntity");
        }
    }

    /**
     * El doble {@code save} de {@code consumeFefo}, fijado como comportamiento
     * esperado.
     *
     * <p>
     * Ese metodo puede guardar <b>dos veces el mismo lote</b> en una sola
     * transaccion: una en el bucle FEFO y otra al llevar el sobrante al ultimo lote
     * de la lista, que con un unico lote disponible es la misma instancia. Es el
     * punto exacto donde apareceria un {@code OptimisticLockException} <i>falso</i>
     * —un conflicto contra uno mismo— si entre los dos {@code merge} llegara a
     * colarse un flush.
     */
    @Nested
    @DisplayName("consumeFefo — el doble save del mismo lote no debe chocar (BE-26)")
    class DobleSaveDelFefo {

        @Test
        @DisplayName("consumir FEFO con sobrante guarda dos veces el mismo lote sin conflicto y sube la version una sola vez")
        void consumir_fefo_con_sobrante_no_lanza_conflicto() {
            Long id = lote("L-UNICO", LocalDate.of(2027, 1, 31), 5).getId();
            releerDesdeLaBase();

            // Bucle FEFO: se lleva las 5 unidades que hay y guarda. consumeFefo descarta
            // el StockLot que devuelve el save, asi que sigue trabajando sobre ESTA
            // instancia, que conserva la version que leyo.
            StockLot unico = repository.findAvailableFefo(PRODUCT, BRANCH).getFirst();
            assertThat(unico.getVersion()).isZero();
            unico.consume(5);
            repository.save(unico);
            asentarEnElKardex(unico, 5);

            // Sobrante: faltan 3 y no hay mas lotes, asi que van contra el ultimo de la
            // lista —el mismo objeto— y se guarda por segunda vez en la misma
            // transaccion, todavia con version 0 en la mano.
            unico.consume(3);
            assertThat(unico.getVersion()).isZero();
            repository.save(unico);
            asentarEnElKardex(unico, 3);

            // El flush es donde saltaria el conflicto falso.
            assertThatCode(() -> releerDesdeLaBase()).doesNotThrowAnyException();

            StockLot releido = repository.findById(id).orElseThrow();
            assertThat(releido.getQuantityAvailable()).isEqualTo(-3);
            assertThat(releido.getVersion()).isEqualTo(1L);
            assertThat(filasDeLoteEnLaBase()).isOne();
        }
    }
}
