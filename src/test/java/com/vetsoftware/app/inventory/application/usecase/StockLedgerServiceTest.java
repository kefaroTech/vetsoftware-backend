package com.vetsoftware.app.inventory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.vetsoftware.app.inventory.application.command.RecordAdjustmentCommand;
import com.vetsoftware.app.inventory.application.command.RecordClinicalUseCommand;
import com.vetsoftware.app.inventory.application.command.RecordPurchaseCommand;
import com.vetsoftware.app.inventory.application.command.RecordSaleCommand;
import com.vetsoftware.app.inventory.application.command.TransferStockCommand;
import com.vetsoftware.app.inventory.application.dto.StockConsumptionDto;
import com.vetsoftware.app.inventory.application.port.out.InventoryMetrics;
import com.vetsoftware.app.inventory.application.port.out.NegativeStockPolicyPort;
import com.vetsoftware.app.inventory.application.port.out.StockBalanceRepository;
import com.vetsoftware.app.inventory.application.port.out.StockLotRepository;
import com.vetsoftware.app.inventory.application.port.out.StockMovementRepository;
import com.vetsoftware.app.inventory.domain.InsufficientStockException;
import com.vetsoftware.app.inventory.domain.StockBalance;
import com.vetsoftware.app.inventory.domain.StockLot;
import com.vetsoftware.app.inventory.domain.StockMovement;
import com.vetsoftware.app.inventory.domain.StockMovementType;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios del {@link StockLedgerService} — el único punto de afectación
 * del kardex.
 *
 * <p>
 * El servicio es <b>stateful</b> (lee → muta → escribe lotes/saldos y descuenta
 * FEFO a través de varios lotes), así que en vez de stubs
 * {@code when/thenReturn} de Mockito (que obligarían a reimplementar el store
 * dentro de {@code thenAnswer}) se usan <b>fakes in-memory</b> de los tres
 * repositorios. Eso permite verificar el <b>estado final real</b>: cantidad
 * disponible por lote, saldo materializado por sede y el ledger completo de
 * movimientos con su signo (entrada +, salida −), tipo, costo por lote (COGS) y
 * referencia. Alineado con el patrón de "stubs manuales" de las convenciones de
 * testing del proyecto.
 *
 * <p>
 * Convenciones de los datos: empresa {@code CO=1}, sedes {@code A=10} /
 * {@code B=20}, producto {@code P=100}.
 */
class StockLedgerServiceTest {

    private static final long CO = 1L;
    private static final long A = 10L;
    private static final long B = 20L;
    private static final long P = 100L;
    private static final long USER = 7L;

    private FakeLotRepository lots;
    private FakeBalanceRepository balances;
    private FakeMovementRepository movements;
    private FakeNegativeStockPolicy policy;
    private StockLedgerService service;

    @BeforeEach
    void setUp() {
        lots = new FakeLotRepository();
        balances = new FakeBalanceRepository();
        movements = new FakeMovementRepository();
        policy = new FakeNegativeStockPolicy(false); // por defecto: bloquear stock negativo
        service = new StockLedgerService(lots, balances, movements, policy,
                mock(InventoryMetrics.class));
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    /**
     * Precarga un lote ya persistido (con id) en una sede y refleja su cantidad en
     * el saldo de esa sede.
     */
    private StockLot seedLot(long branch, int qty, String cost, LocalDate expire,
            String lotNumber) {
        return seedLotFor(P, branch, qty, cost, expire, lotNumber);
    }

    /** Igual que {@link #seedLot}, para un producto distinto de {@code P}. */
    private StockLot seedLotFor(long product, long branch, int qty, String cost, LocalDate expire,
            String lotNumber) {
        StockLot lot = lots.save(new StockLot(null, CO, branch, product, lotNumber, expire, qty,
                bd(cost), null, null, true));
        StockBalance bal = balances.find(product, branch)
                .orElseGet(() -> balances.save(StockBalance.create(CO, branch, product, 0)));
        bal.add(qty);
        balances.save(bal);
        return lot;
    }

    // =================================================================
    // recordPurchase

    @Nested
    class Purchase {

        @Test
        void rechaza_cantidad_cero_y_no_escribe_nada() {
            assertThatThrownBy(() -> service.recordPurchase(purchase(0, "10", null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity must be greater than 0");
            assertThat(lots.all()).isEmpty();
            assertThat(balances.all()).isEmpty();
            assertThat(movements.all()).isEmpty();
        }

        @Test
        void rechaza_cantidad_negativa() {
            assertThatThrownBy(() -> service.recordPurchase(purchase(-5, "10", null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity must be greater than 0");
            assertThat(movements.all()).isEmpty();
        }

        @Test
        void crea_lote_nuevo_saldo_y_movimiento_de_entrada_con_signo_positivo() {
            service.recordPurchase(purchase(50, "30", "L-1", LocalDate.of(2027, 1, 1)));

            assertThat(lots.all()).hasSize(1);
            StockLot lot = lots.all().get(0);
            assertThat(lot.getQuantityAvailable()).isEqualTo(50);
            assertThat(lot.getUnitCost()).isEqualByComparingTo("30");
            assertThat(lot.getLotNumber()).isEqualTo("L-1");
            assertThat(lot.getBranchId()).isEqualTo(A);

            assertThat(balances.qty(P, A)).isEqualTo(50);

            assertThat(movements.all()).hasSize(1);
            StockMovement m = movements.all().get(0);
            assertThat(m.getType()).isEqualTo(StockMovementType.PURCHASE);
            assertThat(m.getQuantity()).as("entrada => signo positivo").isEqualTo(50);
            assertThat(m.getUnitCost()).isEqualByComparingTo("30");
            assertThat(m.getLotId()).isEqualTo(lot.getId());
            assertThat(m.getReferenceType()).isEqualTo(StockReferenceType.GOODS_RECEIPT);
            assertThat(m.getCreatedBy()).isEqualTo(USER);
        }

        @Test
        void costo_null_se_normaliza_a_cero_en_lote_y_movimiento() {
            service.recordPurchase(purchase(10, null, "L-1", null));

            assertThat(lots.all().get(0).getUnitCost()).isEqualByComparingTo("0");
            assertThat(movements.all().get(0).getUnitCost()).isEqualByComparingTo("0");
        }

        @Test
        void acumula_en_lote_existente_de_misma_identidad_sin_crear_otro() {
            StockLot existing = seedLot(A, 20, "30", LocalDate.of(2027, 1, 1), "L-1");
            movements.clear();

            service.recordPurchase(purchase(30, "30", "L-1", LocalDate.of(2027, 1, 1)));

            assertThat(lots.all()).as("misma identidad => un solo lote").hasSize(1);
            assertThat(existing.getQuantityAvailable()).isEqualTo(50);
            assertThat(balances.qty(P, A)).isEqualTo(50);
            assertThat(movements.all()).hasSize(1);
            assertThat(movements.all().get(0).getLotId()).isEqualTo(existing.getId());
        }

        @Test
        void distinto_costo_crea_un_lote_separado() {
            seedLot(A, 20, "30", LocalDate.of(2027, 1, 1), "L-1");
            service.recordPurchase(purchase(10, "35", "L-1", LocalDate.of(2027, 1, 1)));

            assertThat(lots.all()).as("otro costo => otro lote (COGS distinto)").hasSize(2);
            assertThat(balances.qty(P, A)).isEqualTo(30);
        }

        private RecordPurchaseCommand purchase(int qty, String cost, String lotNumber,
                LocalDate expire) {
            return new RecordPurchaseCommand(CO, A, P, lotNumber, expire, qty,
                    cost == null ? null : bd(cost), StockReferenceType.GOODS_RECEIPT, 999L, USER);
        }
    }

    // ================================================================= recordSale

    @Nested
    class Sale {

        @Test
        void rechaza_cantidad_cero() {
            assertThatThrownBy(() -> service.recordSale(sale(0, false)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity must be greater than 0");
        }

        @Test
        void rechaza_cantidad_negativa() {
            assertThatThrownBy(() -> service.recordSale(sale(-3, false)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity must be greater than 0");
        }

        @Test
        void es_idempotente_si_la_referencia_ya_se_registro() {
            seedLot(A, 50, "30", null, "L-1");
            // Simula un intento previo con la misma referencia POS.
            movements.save(StockMovement.of(CO, A, P, 1L, StockMovementType.SALE, 5, bd("30"),
                    StockReferenceType.POS_DOCUMENT, 555L, null, USER));
            int movementsBefore = movements.all().size();

            List<StockConsumptionDto> result = service.recordSale(sale(5, false));

            assertThat(result).as("reintento no descuenta de nuevo").isEmpty();
            assertThat(movements.all()).hasSize(movementsBefore);
            assertThat(balances.qty(P, A)).isEqualTo(50);
        }

        @Test
        void descuenta_de_un_lote_y_devuelve_el_consumo_por_lote() {
            StockLot lot = seedLot(A, 50, "30", null, "L-1");
            movements.clear();

            List<StockConsumptionDto> result = service.recordSale(sale(8, false));

            assertThat(lot.getQuantityAvailable()).isEqualTo(42);
            assertThat(balances.qty(P, A)).isEqualTo(42);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).lotId()).isEqualTo(lot.getId());
            assertThat(result.get(0).quantity()).isEqualTo(8);
            assertThat(result.get(0).unitCost()).isEqualByComparingTo("30");

            StockMovement m = movements.all().get(0);
            assertThat(m.getType()).isEqualTo(StockMovementType.SALE);
            assertThat(m.getQuantity()).as("salida => signo negativo").isEqualTo(-8);
        }

        @Test
        void consume_fefo_vencimiento_mas_proximo_primero_a_traves_de_varios_lotes() {
            // Lote que vence antes (L-EARLY) debe consumirse completo antes de tocar el que
            // vence
            // después.
            StockLot late = seedLot(A, 10, "35", LocalDate.of(2027, 12, 1), "L-LATE");
            StockLot early = seedLot(A, 5, "30", LocalDate.of(2027, 1, 1), "L-EARLY");
            movements.clear();

            List<StockConsumptionDto> result = service.recordSale(sale(8, false));

            assertThat(early.getQuantityAvailable()).as("el que vence antes se agota primero")
                    .isZero();
            assertThat(late.getQuantityAvailable()).isEqualTo(7);
            assertThat(balances.qty(P, A)).isEqualTo(7);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).lotId()).isEqualTo(early.getId());
            assertThat(result.get(0).quantity()).isEqualTo(5);
            assertThat(result.get(0).unitCost()).isEqualByComparingTo("30");
            assertThat(result.get(1).lotId()).isEqualTo(late.getId());
            assertThat(result.get(1).quantity()).isEqualTo(3);
            assertThat(result.get(1).unitCost()).isEqualByComparingTo("35");
            assertThat(movements.ofType(StockMovementType.SALE)).hasSize(2);
        }

        @Test
        void lotes_sin_vencimiento_van_al_final_del_orden_fefo() {
            StockLot noExpiry = seedLot(A, 5, "30", null, "L-NULL");
            StockLot dated = seedLot(A, 5, "30", LocalDate.of(2027, 1, 1), "L-DATED");
            movements.clear();

            List<StockConsumptionDto> result = service.recordSale(sale(6, false));

            assertThat(dated.getQuantityAvailable()).as("con fecha primero").isZero();
            assertThat(noExpiry.getQuantityAvailable()).as("sin fecha al final").isEqualTo(4);
            assertThat(result.get(0).lotId()).isEqualTo(dated.getId());
            assertThat(result.get(1).lotId()).isEqualTo(noExpiry.getId());
        }

        @Test
        void permite_vender_exactamente_todo_el_stock_dejando_saldo_en_cero() {
            seedLot(A, 10, "30", null, "L-1");
            List<StockConsumptionDto> result = service.recordSale(sale(10, false));
            assertThat(balances.qty(P, A)).isZero();
            assertThat(result).hasSize(1);
        }

        @Test
        void rechaza_venta_sin_stock_suficiente_cuando_no_se_permite_negativo() {
            seedLot(A, 3, "30", null, "L-1");
            movements.clear();

            assertThatThrownBy(() -> service.recordSale(sale(5, false)))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Stock insuficiente")
                    .hasMessageContaining("producto " + P).hasMessageContaining("sede " + A)
                    .hasMessageContaining("disponible 3").hasMessageContaining("requerido 5");

            assertThat(balances.qty(P, A)).as("no se toca el saldo").isEqualTo(3);
            assertThat(movements.all()).as("no se registra movimiento").isEmpty();
        }

        @Test
        void pos_fuerza_negativo_aunque_la_politica_lo_prohiba() {
            StockLot lot = seedLot(A, 3, "30", null, "L-1");
            movements.clear();
            policy.allow = false; // la política prohíbe, pero POS manda allowNegative=true

            List<StockConsumptionDto> result = service.recordSale(sale(5, true));

            assertThat(balances.qty(P, A)).as("saldo puede quedar negativo en POS").isEqualTo(-2);
            assertThat(lot.getQuantityAvailable()).isEqualTo(-2);
            // 3 del stock + 2 de sobregiro sobre el mismo (último) lote => 2 movimientos de
            // salida.
            assertThat(movements.ofType(StockMovementType.SALE)).hasSize(2);
            assertThat(result).hasSize(2);
            assertThat(result.get(0).quantity()).isEqualTo(3);
            assertThat(result.get(1).quantity()).isEqualTo(2);
        }

        @Test
        void cuenta_abierta_respeta_el_flag_de_empresa_cuando_esta_activo() {
            seedLot(A, 3, "30", null, "L-1");
            policy.allow = true; // la empresa permite negativo

            List<StockConsumptionDto> result = service.recordSale(sale(5, false)); // allowNegative=false

            assertThat(balances.qty(P, A)).isEqualTo(-2);
            assertThat(result).isNotEmpty();
        }

        @Test
        void venta_sin_ningun_lote_y_negativo_permitido_crea_lote_generico_en_cero() {
            // Sin lotes en la sede; saldo materializado no existe (arranca 0).
            policy.allow = true;

            List<StockConsumptionDto> result = service.recordSale(sale(4, false));

            assertThat(lots.all()).as("se crea un lote genérico para colgar el sobregiro")
                    .hasSize(1);
            StockLot generic = lots.all().get(0);
            assertThat(generic.getLotNumber()).isNull();
            assertThat(generic.getUnitCost()).isEqualByComparingTo("0");
            assertThat(generic.getQuantityAvailable()).isEqualTo(-4);
            assertThat(balances.qty(P, A)).isEqualTo(-4);
            assertThat(result).hasSize(1);
        }

        /**
         * Regresión de BE-01. Una venta POS de varios productos emite un
         * {@code recordSale} por línea con el MISMO id de documento como referencia.
         * Cuando la clave de idempotencia era solo (tipo, referencia), el INSERT de la
         * primera línea —visible en la transacción porque el id es IDENTITY— hacía que
         * la segunda saliera como duplicado: sin error, sin descuento y con el
         * inventario sobrestimado.
         */
        @Test
        void venta_multiproducto_descuenta_todas_las_lineas_bajo_el_mismo_documento() {
            long otroProducto = 200L;
            seedLot(A, 50, "30", null, "L-P");
            seedLotFor(otroProducto, A, 40, "12", null, "L-OTRO");
            movements.clear();

            service.recordSale(sale(5, false));
            List<StockConsumptionDto> segunda = service.recordSale(new RecordSaleCommand(CO, A,
                    otroProducto, 3, StockReferenceType.POS_DOCUMENT, 555L, USER, false));

            assertThat(segunda).as("la segunda línea no puede descartarse como duplicado")
                    .isNotEmpty();
            assertThat(balances.qty(P, A)).isEqualTo(45);
            assertThat(balances.qty(otroProducto, A)).as("el segundo producto también descuenta")
                    .isEqualTo(37);
            assertThat(movements.all()).hasSize(2);
        }

        /**
         * El producto entra en la clave, pero la referencia sigue mandando: reenviar la
         * misma línea del mismo documento no puede descontar dos veces.
         */
        @Test
        void reintento_de_la_misma_linea_sigue_siendo_idempotente() {
            long otroProducto = 200L;
            seedLot(A, 50, "30", null, "L-P");
            seedLotFor(otroProducto, A, 40, "12", null, "L-OTRO");
            movements.clear();

            service.recordSale(sale(5, false));
            service.recordSale(new RecordSaleCommand(CO, A, otroProducto, 3,
                    StockReferenceType.POS_DOCUMENT, 555L, USER, false));
            List<StockConsumptionDto> reintento = service.recordSale(sale(5, false));

            assertThat(reintento).as("mismo documento y mismo producto: duplicado").isEmpty();
            assertThat(balances.qty(P, A)).isEqualTo(45);
            assertThat(balances.qty(otroProducto, A)).isEqualTo(37);
            assertThat(movements.all()).hasSize(2);
        }

        /**
         * Un solo producto puede consumir varios lotes por FEFO y emitir un movimiento
         * por lote, todos con la misma terna (tipo, referencia, producto). Es la razón
         * por la que la clave de idempotencia vive en la aplicación y no puede
         * expresarse como un índice único sobre esa terna.
         */
        @Test
        void una_linea_que_cruza_varios_lotes_emite_un_movimiento_por_lote() {
            seedLot(A, 4, "30", LocalDate.now().plusDays(10), "L-EARLY");
            seedLot(A, 10, "31", LocalDate.now().plusDays(90), "L-LATE");
            movements.clear();

            List<StockConsumptionDto> result = service.recordSale(sale(9, false));

            assertThat(result).as("dos lotes consumidos").hasSize(2);
            assertThat(movements.all()).as("un movimiento de kardex por lote").hasSize(2);
            assertThat(movements.all()).allSatisfy(m -> {
                assertThat(m.getReferenceType()).isEqualTo(StockReferenceType.POS_DOCUMENT);
                assertThat(m.getReferenceId()).isEqualTo(555L);
                assertThat(m.getProductId()).isEqualTo(P);
            });
        }

        private RecordSaleCommand sale(int qty, boolean allowNegative) {
            return new RecordSaleCommand(CO, A, P, qty, StockReferenceType.POS_DOCUMENT, 555L, USER,
                    allowNegative);
        }
    }

    // =================================================================
    // recordClinicalUse

    @Nested
    class ClinicalUse {

        @Test
        void rechaza_cantidad_cero() {
            assertThatThrownBy(() -> service.recordClinicalUse(clinical(0, 42L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity must be greater than 0");
        }

        @Test
        void descuenta_fefo_con_tipo_y_referencia_clinica() {
            StockLot lot = seedLot(A, 10, "30", null, "L-1");
            movements.clear();

            List<StockConsumptionDto> result = service.recordClinicalUse(clinical(3, 42L));

            assertThat(lot.getQuantityAvailable()).isEqualTo(7);
            assertThat(balances.qty(P, A)).isEqualTo(7);
            assertThat(result).hasSize(1);

            StockMovement m = movements.all().get(0);
            assertThat(m.getType()).isEqualTo(StockMovementType.CLINICAL_USE);
            assertThat(m.getQuantity()).isEqualTo(-3);
            assertThat(m.getReferenceType()).isEqualTo(StockReferenceType.CLINICAL_EVENT);
            assertThat(m.getReferenceId()).isEqualTo(42L);
            assertThat(m.getReason()).isEqualTo("vacuna aplicada");
        }

        @Test
        void es_idempotente_por_evento_clinico_cuando_hay_referencia() {
            seedLot(A, 10, "30", null, "L-1");
            movements.save(StockMovement.of(CO, A, P, 1L, StockMovementType.CLINICAL_USE, 3,
                    bd("30"), StockReferenceType.CLINICAL_EVENT, 42L, null, USER));
            int before = movements.all().size();

            List<StockConsumptionDto> result = service.recordClinicalUse(clinical(3, 42L));

            assertThat(result).isEmpty();
            assertThat(movements.all()).hasSize(before);
            assertThat(balances.qty(P, A)).isEqualTo(10);
        }

        @Test
        void consumo_manual_sin_referencia_no_aplica_idempotencia() {
            seedLot(A, 10, "30", null, "L-1");
            // Aunque exista un CLINICAL_EVENT con referencia null previo, un consumo manual
            // (ref null)
            // siempre corre.
            movements.save(StockMovement.of(CO, A, P, 1L, StockMovementType.CLINICAL_USE, 3,
                    bd("30"), StockReferenceType.CLINICAL_EVENT, null, null, USER));

            List<StockConsumptionDto> result = service.recordClinicalUse(clinical(3, null));

            assertThat(result).as("manual siempre descuenta").hasSize(1);
            assertThat(balances.qty(P, A)).isEqualTo(7);
        }

        @Test
        void rechaza_sin_stock_cuando_la_politica_prohibe_negativo() {
            seedLot(A, 1, "30", null, "L-1");
            movements.clear();
            policy.allow = false;

            assertThatThrownBy(() -> service.recordClinicalUse(clinical(2, 42L)))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("consumo clínico").hasMessageContaining("disponible 1")
                    .hasMessageContaining("requerido 2");

            assertThat(balances.qty(P, A)).isEqualTo(1);
            assertThat(movements.all()).isEmpty();
        }

        @Test
        void permite_negativo_si_la_empresa_lo_habilita() {
            seedLot(A, 1, "30", null, "L-1");
            policy.allow = true;

            service.recordClinicalUse(clinical(2, 42L));

            assertThat(balances.qty(P, A)).isEqualTo(-1);
        }

        private RecordClinicalUseCommand clinical(int qty, Long referenceId) {
            return new RecordClinicalUseCommand(CO, A, P, qty, referenceId, "vacuna aplicada",
                    USER);
        }
    }

    // =================================================================
    // recordAdjustment

    @Nested
    class Adjustment {

        @Test
        void rechaza_delta_cero() {
            assertThatThrownBy(() -> service.recordAdjustment(adj(0, "10", "conteo")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("delta cannot be 0");
        }

        @Test
        void rechaza_motivo_null() {
            assertThatThrownBy(() -> service.recordAdjustment(adj(5, "10", null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required");
        }

        @Test
        void rechaza_motivo_en_blanco() {
            assertThatThrownBy(() -> service.recordAdjustment(adj(5, "10", "   ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required");
        }

        @Test
        void ajuste_de_entrada_crea_lote_generico_y_movimiento_adjustment_in() {
            service.recordAdjustment(adj(7, "12", "conteo físico"));

            assertThat(lots.all()).hasSize(1);
            StockLot lot = lots.all().get(0);
            assertThat(lot.getLotNumber()).as("ajuste => lote genérico sin número").isNull();
            assertThat(lot.getExpireDate()).isNull();
            assertThat(lot.getQuantityAvailable()).isEqualTo(7);
            assertThat(lot.getUnitCost()).isEqualByComparingTo("12");
            assertThat(balances.qty(P, A)).isEqualTo(7);

            StockMovement m = movements.all().get(0);
            assertThat(m.getType()).isEqualTo(StockMovementType.ADJUSTMENT_IN);
            assertThat(m.getQuantity()).isEqualTo(7);
            assertThat(m.getReferenceType()).isEqualTo(StockReferenceType.ADJUSTMENT);
            assertThat(m.getReason()).isEqualTo("conteo físico");
        }

        @Test
        void ajuste_de_entrada_con_costo_null_usa_cero() {
            service.recordAdjustment(adj(3, null, "conteo"));
            assertThat(lots.all().get(0).getUnitCost()).isEqualByComparingTo("0");
        }

        @Test
        void ajuste_de_salida_descuenta_fefo_y_registra_adjustment_out() {
            StockLot lot = seedLot(A, 10, "30", null, "L-1");
            movements.clear();

            service.recordAdjustment(adj(-4, null, "merma"));

            assertThat(lot.getQuantityAvailable()).isEqualTo(6);
            assertThat(balances.qty(P, A)).isEqualTo(6);
            StockMovement m = movements.all().get(0);
            assertThat(m.getType()).isEqualTo(StockMovementType.ADJUSTMENT_OUT);
            assertThat(m.getQuantity()).isEqualTo(-4);
        }

        @Test
        void ajuste_de_salida_puede_dejar_negativo_sin_consultar_la_politica() {
            seedLot(A, 2, "30", null, "L-1");
            policy.allow = false; // aun prohibiendo negativo, el ajuste de conteo SÍ puede corregir
                                  // a negativo

            service.recordAdjustment(adj(-5, null, "corrección de conteo"));

            assertThat(balances.qty(P, A)).isEqualTo(-3);
            assertThat(policy.queried).as("los ajustes no consultan la política de negativo")
                    .isFalse();
        }

        private RecordAdjustmentCommand adj(int delta, String cost, String reason) {
            return new RecordAdjustmentCommand(CO, A, P, delta, cost == null ? null : bd(cost),
                    reason, 777L, USER);
        }
    }

    // ================================================================= transfer

    @Nested
    class Transfer {

        @Test
        void rechaza_cantidad_cero() {
            assertThatThrownBy(() -> service.transfer(transfer(A, B, 0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity must be greater than 0");
        }

        @Test
        void rechaza_misma_sede_origen_y_destino() {
            assertThatThrownBy(() -> service.transfer(transfer(A, A, 5)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("misma sede");
        }

        @Test
        void rechaza_transferencia_sin_stock_suficiente_en_origen() {
            seedLot(A, 3, "30", null, "L-1");
            movements.clear();

            assertThatThrownBy(() -> service.transfer(transfer(A, B, 5)))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("transferir").hasMessageContaining("disponible 3")
                    .hasMessageContaining("requerido 5");

            assertThat(balances.qty(P, A)).isEqualTo(3);
            assertThat(balances.qty(P, B)).isZero();
            assertThat(movements.all()).isEmpty();
        }

        @Test
        void mueve_saldo_y_replica_el_lote_en_destino_con_misma_identidad() {
            LocalDate exp = LocalDate.of(2027, 6, 1);
            StockLot origin = seedLot(A, 50, "30", exp, "L-1");
            movements.clear();

            service.transfer(transfer(A, B, 20));

            assertThat(origin.getQuantityAvailable()).isEqualTo(30);
            assertThat(balances.qty(P, A)).isEqualTo(30);
            assertThat(balances.qty(P, B)).isEqualTo(20);

            // El lote destino conserva número/vencimiento/costo (preserva FEFO y COGS).
            StockLot dest = lots.all().stream().filter(l -> l.getBranchId() == B).findFirst()
                    .orElseThrow();
            assertThat(dest.getQuantityAvailable()).isEqualTo(20);
            assertThat(dest.getLotNumber()).isEqualTo("L-1");
            assertThat(dest.getExpireDate()).isEqualTo(exp);
            assertThat(dest.getUnitCost()).isEqualByComparingTo("30");

            assertThat(movements.ofType(StockMovementType.TRANSFER_OUT)).hasSize(1);
            assertThat(movements.ofType(StockMovementType.TRANSFER_IN)).hasSize(1);
            assertThat(movements.ofType(StockMovementType.TRANSFER_OUT).get(0).getQuantity())
                    .isEqualTo(-20);
            assertThat(movements.ofType(StockMovementType.TRANSFER_IN).get(0).getQuantity())
                    .isEqualTo(20);
        }

        @Test
        void transfiere_fefo_a_traves_de_varios_lotes_conservando_costo_por_lote() {
            StockLot early = seedLot(A, 5, "30", LocalDate.of(2027, 1, 1), "L-EARLY");
            StockLot late = seedLot(A, 10, "35", LocalDate.of(2027, 12, 1), "L-LATE");
            movements.clear();

            service.transfer(transfer(A, B, 8));

            assertThat(early.getQuantityAvailable()).isZero();
            assertThat(late.getQuantityAvailable()).isEqualTo(7);
            assertThat(balances.qty(P, A)).isEqualTo(7);
            assertThat(balances.qty(P, B)).isEqualTo(8);

            // Se replican DOS lotes en destino, cada uno con su costo original.
            List<StockLot> destLots = lots.all().stream().filter(l -> l.getBranchId() == B)
                    .toList();
            assertThat(destLots).hasSize(2);
            assertThat(destLots).anySatisfy(l -> {
                assertThat(l.getUnitCost()).isEqualByComparingTo("30");
                assertThat(l.getQuantityAvailable()).isEqualTo(5);
            });
            assertThat(destLots).anySatisfy(l -> {
                assertThat(l.getUnitCost()).isEqualByComparingTo("35");
                assertThat(l.getQuantityAvailable()).isEqualTo(3);
            });
            assertThat(movements.ofType(StockMovementType.TRANSFER_OUT)).hasSize(2);
            assertThat(movements.ofType(StockMovementType.TRANSFER_IN)).hasSize(2);
        }

        @Test
        void acumula_en_lote_destino_existente_de_misma_identidad() {
            LocalDate exp = LocalDate.of(2027, 6, 1);
            seedLot(A, 50, "30", exp, "L-1");
            StockLot destExisting = seedLot(B, 5, "30", exp, "L-1");
            movements.clear();

            service.transfer(transfer(A, B, 10));

            assertThat(lots.all().stream().filter(l -> l.getBranchId() == B).toList())
                    .as("no se duplica el lote en destino").hasSize(1);
            assertThat(destExisting.getQuantityAvailable()).isEqualTo(15);
            assertThat(balances.qty(P, B)).isEqualTo(15);
        }

        @Test
        void funciona_igual_cuando_el_id_de_origen_es_mayor_que_el_de_destino() {
            // Ejercita la rama de lock ordenado inverso (from > to): el resultado debe ser
            // idéntico.
            seedLot(B, 40, "30", null, "L-1");

            service.transfer(new TransferStockCommand(CO, B, A, P, 15, "rebalanceo", USER));

            assertThat(balances.qty(P, B)).isEqualTo(25);
            assertThat(balances.qty(P, A)).isEqualTo(15);
        }

        @Test
        void aborta_si_los_lotes_no_alcanzan_aunque_el_saldo_diga_que_si() {
            // Saldo materializado (30) diverge de los lotes reales (10): la transferencia
            // debe abortar,
            // nunca crear stock negativo en origen por una inconsistencia.
            seedLot(A, 10, "30", null, "L-1");
            StockBalance bal = balances.find(P, A).orElseThrow();
            bal.add(20); // fuerza divergencia saldo=30 vs lotes=10
            balances.save(bal);
            movements.clear();

            assertThatThrownBy(() -> service.transfer(transfer(A, B, 25)))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Lotes insuficientes");
        }

        private TransferStockCommand transfer(long from, long to, int qty) {
            return new TransferStockCommand(CO, from, to, P, qty, "traslado", USER);
        }
    }

    // ================================================================= reverse

    @Nested
    class Reverse {

        @Test
        void sin_movimientos_no_hace_nada() {
            service.reverse(StockReferenceType.POS_DOCUMENT, 999L, CO, USER);
            assertThat(movements.all()).isEmpty();
        }

        @Test
        void reversa_de_una_venta_repone_stock_al_mismo_lote_con_void_in() {
            StockLot lot = seedLot(A, 42, "30", null, "L-1"); // saldo tras vender 8 de 50
            // Movimiento original de salida por venta.
            movements.save(StockMovement.of(CO, A, P, lot.getId(), StockMovementType.SALE, 8,
                    bd("30"), StockReferenceType.POS_DOCUMENT, 555L, null, USER));

            service.reverse(StockReferenceType.POS_DOCUMENT, 555L, CO, USER);

            assertThat(lot.getQuantityAvailable()).as("se repone el lote").isEqualTo(50);
            assertThat(balances.qty(P, A)).isEqualTo(50);
            List<StockMovement> voids = movements.ofType(StockMovementType.VOID_IN);
            assertThat(voids).hasSize(1);
            assertThat(voids.get(0).getQuantity()).as("VOID_IN es entrada => positivo")
                    .isEqualTo(8);
            assertThat(voids.get(0).getReason()).isEqualTo("reversa");
        }

        @Test
        void reversa_de_una_compra_retira_stock_con_void_out() {
            StockLot lot = seedLot(A, 50, "30", null, "L-1");
            movements.save(StockMovement.of(CO, A, P, lot.getId(), StockMovementType.PURCHASE, 50,
                    bd("30"), StockReferenceType.GOODS_RECEIPT, 321L, null, USER));

            service.reverse(StockReferenceType.GOODS_RECEIPT, 321L, CO, USER);

            assertThat(lot.getQuantityAvailable()).isZero();
            assertThat(balances.qty(P, A)).isZero();
            List<StockMovement> voids = movements.ofType(StockMovementType.VOID_OUT);
            assertThat(voids).hasSize(1);
            assertThat(voids.get(0).getQuantity()).as("VOID_OUT es salida => negativo")
                    .isEqualTo(-50);
        }

        @Test
        void reversa_de_venta_multi_lote_repone_cada_lote() {
            StockLot early = seedLot(A, 0, "30", LocalDate.of(2027, 1, 1), "L-EARLY"); // se agotó
                                                                                       // vendiendo
                                                                                       // 5
            StockLot late = seedLot(A, 7, "35", LocalDate.of(2027, 12, 1), "L-LATE"); // quedó en 7
                                                                                      // tras vender
                                                                                      // 3
            movements.save(StockMovement.of(CO, A, P, early.getId(), StockMovementType.SALE, 5,
                    bd("30"), StockReferenceType.POS_DOCUMENT, 555L, null, USER));
            movements.save(StockMovement.of(CO, A, P, late.getId(), StockMovementType.SALE, 3,
                    bd("35"), StockReferenceType.POS_DOCUMENT, 555L, null, USER));

            service.reverse(StockReferenceType.POS_DOCUMENT, 555L, CO, USER);

            assertThat(early.getQuantityAvailable()).isEqualTo(5);
            assertThat(late.getQuantityAvailable()).isEqualTo(10);
            assertThat(balances.qty(P, A)).isEqualTo(15);
            assertThat(movements.ofType(StockMovementType.VOID_IN)).hasSize(2);
        }

        @Test
        void es_idempotente_si_ya_fue_reversado() {
            StockLot lot = seedLot(A, 50, "30", null, "L-1");
            movements.save(StockMovement.of(CO, A, P, lot.getId(), StockMovementType.SALE, 8,
                    bd("30"), StockReferenceType.POS_DOCUMENT, 555L, null, USER));
            movements.save(StockMovement.of(CO, A, P, lot.getId(), StockMovementType.VOID_IN, 8,
                    bd("30"), StockReferenceType.POS_DOCUMENT, 555L, "reversa", USER));
            int before = movements.all().size();
            int lotQtyBefore = lot.getQuantityAvailable();

            service.reverse(StockReferenceType.POS_DOCUMENT, 555L, CO, USER);

            assertThat(movements.all()).as("no vuelve a compensar").hasSize(before);
            assertThat(lot.getQuantityAvailable()).isEqualTo(lotQtyBefore);
        }

        @Test
        void ignora_movimientos_cuyo_lote_ya_no_existe() {
            // Movimiento que apunta a un lote inexistente (id 9999) => se salta sin fallar.
            movements.save(StockMovement.of(CO, A, P, 9999L, StockMovementType.SALE, 8, bd("30"),
                    StockReferenceType.POS_DOCUMENT, 555L, null, USER));

            service.reverse(StockReferenceType.POS_DOCUMENT, 555L, CO, USER);

            assertThat(movements.ofType(StockMovementType.VOID_IN)).isEmpty();
            assertThat(movements.ofType(StockMovementType.VOID_OUT)).isEmpty();
        }
    }

    // ================================================================= fakes

    /**
     * Repositorio de lotes in-memory: mantiene las MISMAS referencias que el
     * servicio muta.
     */
    private static final class FakeLotRepository implements StockLotRepository {
        private final Map<Long, StockLot> store = new LinkedHashMap<>();
        private long seq = 0;

        @Override
        public StockLot save(StockLot lot) {
            if (lot.getId() == null)
                lot.assignId(++seq);
            store.put(lot.getId(), lot);
            return lot;
        }

        @Override
        public Optional<StockLot> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<StockLot> findAvailableFefo(Long productId, Long branchId) {
            List<StockLot> result = new ArrayList<>(
                    store.values().stream().filter(l -> l.isEnabled())
                            .filter(l -> Objects.equals(l.getProductId(), productId))
                            .filter(l -> Objects.equals(l.getBranchId(), branchId))
                            .filter(l -> l.getQuantityAvailable() > 0).toList());
            result.sort(Comparator
                    .comparing(StockLot::getExpireDate,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(StockLot::getId));
            return result;
        }

        @Override
        public Optional<StockLot> findByIdentity(Long companyId, Long branchId, Long productId,
                String lotNumber, LocalDate expireDate, BigDecimal unitCost) {
            return store.values().stream().filter(l -> Objects.equals(l.getCompanyId(), companyId))
                    .filter(l -> Objects.equals(l.getBranchId(), branchId))
                    .filter(l -> Objects.equals(l.getProductId(), productId))
                    .filter(l -> Objects.equals(l.getLotNumber(), lotNumber))
                    .filter(l -> Objects.equals(l.getExpireDate(), expireDate))
                    .filter(l -> l.getUnitCost().compareTo(unitCost) == 0).findFirst();
        }

        List<StockLot> all() {
            return new ArrayList<>(store.values());
        }
    }

    /**
     * Saldos in-memory keyed por (producto, sede). {@code findForUpdate} ==
     * {@code find} (no hay lock real en test).
     */
    private static final class FakeBalanceRepository implements StockBalanceRepository {
        private final Map<String, StockBalance> store = new LinkedHashMap<>();
        private long seq = 0;

        private static String key(Long productId, Long branchId) {
            return productId + ":" + branchId;
        }

        @Override
        public StockBalance save(StockBalance balance) {
            if (balance.getId() == null)
                balance.assignId(++seq);
            store.put(key(balance.getProductId(), balance.getBranchId()), balance);
            return balance;
        }

        @Override
        public Optional<StockBalance> find(Long productId, Long branchId) {
            return Optional.ofNullable(store.get(key(productId, branchId)));
        }

        @Override
        public Optional<StockBalance> findForUpdate(Long productId, Long branchId) {
            return find(productId, branchId);
        }

        int qty(Long productId, Long branchId) {
            return find(productId, branchId).map(StockBalance::getQuantity).orElse(0);
        }

        List<StockBalance> all() {
            return new ArrayList<>(store.values());
        }
    }

    /** Ledger append-only in-memory con consulta por referencia. */
    private static final class FakeMovementRepository implements StockMovementRepository {
        private final List<StockMovement> saved = new ArrayList<>();
        private long seq = 0;

        @Override
        public StockMovement save(StockMovement movement) {
            if (movement.getId() == null)
                movement.assignId(++seq);
            saved.add(movement);
            return movement;
        }

        @Override
        public boolean existsByReference(StockReferenceType referenceType, Long referenceId,
                Long productId) {
            return saved.stream()
                    .anyMatch(m -> m.getReferenceType() == referenceType
                            && Objects.equals(m.getReferenceId(), referenceId)
                            && Objects.equals(m.getProductId(), productId));
        }

        @Override
        public List<StockMovement> findByReferenceAndCompanyId(StockReferenceType referenceType,
                Long referenceId, Long companyId) {
            return saved.stream()
                    .filter(m -> m.getReferenceType() == referenceType
                            && Objects.equals(m.getReferenceId(), referenceId)
                            && Objects.equals(m.getCompanyId(), companyId))
                    .toList();
        }

        List<StockMovement> all() {
            return new ArrayList<>(saved);
        }

        List<StockMovement> ofType(StockMovementType type) {
            return saved.stream().filter(m -> m.getType() == type).toList();
        }

        void clear() {
            saved.clear();
        }
    }

    /** Política de stock negativo configurable; registra si fue consultada. */
    private static final class FakeNegativeStockPolicy implements NegativeStockPolicyPort {
        private boolean allow;
        private boolean queried;

        private FakeNegativeStockPolicy(boolean allow) {
            this.allow = allow;
        }

        @Override
        public boolean allowsNegative(Long companyId) {
            this.queried = true;
            return allow;
        }
    }
}
