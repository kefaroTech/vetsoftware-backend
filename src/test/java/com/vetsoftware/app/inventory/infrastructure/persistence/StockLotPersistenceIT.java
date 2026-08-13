package com.vetsoftware.app.inventory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.inventory.domain.StockLot;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
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
@Import(JpaStockLotRepository.class)
@DisplayName("JpaStockLotRepository — FEFO e identidad de lote contra MySQL real")
class StockLotPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;
    private static final Long OTRA_BRANCH = SchemaSeed.OTRA_BRANCH_ID;
    private static final Long PRODUCT = SchemaSeed.PRODUCT_ID;
    private static final BigDecimal COSTO = new BigDecimal("1500.0000");

    @Autowired
    private JpaStockLotRepository repository;

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
        LocalDateTime ahora = LocalDateTime.of(2026, 1, 15, 10, 30);
        return repository.save(new StockLot(null, COMPANY, branchId, PRODUCT, numero, vence,
                disponible, costo, ahora, ahora, habilitado));
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
}
