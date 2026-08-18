package com.vetsoftware.app.inventory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.inventory.application.command.SearchKardexCommand;
import com.vetsoftware.app.inventory.application.command.SearchPurchasesQuery;
import com.vetsoftware.app.inventory.application.command.SearchStockCommand;
import com.vetsoftware.app.inventory.application.dto.InventoryAlertsView;
import com.vetsoftware.app.inventory.application.dto.InventoryValuationView;
import com.vetsoftware.app.inventory.application.dto.KardexExportRow;
import com.vetsoftware.app.inventory.application.dto.PurchaseView;
import com.vetsoftware.app.inventory.application.dto.StockLotView;
import com.vetsoftware.app.inventory.application.dto.StockView;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja del read model de inventario contra MySQL real.
 *
 * <p>
 * Es el adaptador con mas SQL crudo del proyecto: joins nativos a
 * {@code products} y {@code branches}, agregados de valuacion, {@code DATEDIFF}
 * para el vencimiento y filtros opcionales resueltos con
 * {@code (:param IS NULL OR ...)}. Nada de eso se puede verificar con un doble,
 * porque lo que se prueba ES la consulta.
 *
 * <p>
 * <b>Hallazgo al escribir estas pruebas.</b> Las alertas de vencimiento mezclan
 * dos relojes: el limite de la ventana lo calcula la JVM
 * ({@code LocalDate.now().plusDays(n)}) y los dias restantes los calcula MySQL
 * ({@code DATEDIFF(expire_date, CURRENT_DATE)}). Si la aplicacion y la base
 * corren en husos distintos —el caso de este contenedor en UTC frente a una JVM
 * en Bogota— filtro y cuenta usan dos "hoy" diferentes y el numero mostrado se
 * desvia un dia durante las ultimas horas de cada jornada. No se arregla aqui
 * porque exige decidir el huso canonico de la aplicacion; queda anotado y las
 * aserciones se escriben sin depender del huso.
 */
@Import(StockQueryAdapter.class)
@DisplayName("StockQueryAdapter — el read model de inventario contra MySQL real")
class StockQueryAdapterIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;
    private static final Long OTRA_BRANCH = SchemaSeed.OTRA_BRANCH_ID;
    private static final Long PRODUCT = SchemaSeed.PRODUCT_ID;
    private static final Long OTRO_PRODUCT = SchemaSeed.OTRO_PRODUCT_ID;
    private static final BigDecimal COSTO = new BigDecimal("1500.0000");

    private static final LocalDateTime ENERO_10 = LocalDateTime.of(2026, 1, 10, 9, 0);
    private static final LocalDateTime ENERO_20 = LocalDateTime.of(2026, 1, 20, 9, 0);
    private static final LocalDateTime FEBRERO_5 = LocalDateTime.of(2026, 2, 5, 9, 0);

    @Autowired
    private StockQueryAdapter adapter;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private void saldo(Long productId, Long branchId, int cantidad, int minimo) {
        entityManager.createNativeQuery("""
                INSERT INTO stock_balance (company_id, branch_id, product_id, quantity, min_stock,
                                           version)
                VALUES (?, ?, ?, ?, ?, 0)
                """).setParameter(1, COMPANY).setParameter(2, branchId).setParameter(3, productId)
                .setParameter(4, cantidad).setParameter(5, minimo).executeUpdate();
    }

    private Long lote(Long productId, Long branchId, LocalDate vence, int disponible,
            BigDecimal costo) {
        entityManager.createNativeQuery("""
                INSERT INTO stock_lot (company_id, branch_id, product_id, lot_number, expire_date,
                                       quantity_available, unit_cost, received_date, created_date,
                                       enabled)
                VALUES (?, ?, ?, 'L-1', ?, ?, ?, ?, ?, true)
                """).setParameter(1, COMPANY).setParameter(2, branchId).setParameter(3, productId)
                .setParameter(4, vence).setParameter(5, disponible).setParameter(6, costo)
                .setParameter(7, ENERO_10).setParameter(8, ENERO_10).executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()")
                .getSingleResult()).longValue();
    }

    private void movimiento(Long productId, Long branchId, Long lotId, String tipo, int cantidad,
            LocalDateTime cuando) {
        entityManager.createNativeQuery("""
                INSERT INTO stock_movement (company_id, branch_id, product_id, lot_id, type,
                                            quantity, unit_cost, reference_type, reference_id,
                                            reason, created_by, created_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'GOODS_RECEIPT', 900, 'prueba', ?, ?)
                """).setParameter(1, COMPANY).setParameter(2, branchId).setParameter(3, productId)
                .setParameter(4, lotId).setParameter(5, tipo).setParameter(6, cantidad)
                .setParameter(7, COSTO).setParameter(8, SchemaSeed.EMPLOYEE_ID)
                .setParameter(9, cuando).executeUpdate();
    }

    @Nested
    @DisplayName("searchStock — el saldo con nombres resueltos")
    class BusquedaDeSaldo {

        @Test
        @DisplayName("resuelve el nombre del producto y de la sede en la misma consulta")
        void resuelve_los_nombres_en_la_misma_consulta() {
            saldo(PRODUCT, BRANCH, 12, 5);

            List<StockView> filas = adapter
                    .searchStock(new SearchStockCommand(COMPANY, null, null, false, 0, 20))
                    .content();

            // El join va en la query: resolverlo en Java seria un N+1 por cada fila del
            // listado de stock.
            assertThat(filas).singleElement().satisfies(f -> {
                assertThat(f.productName()).isEqualTo("Amoxicilina 500mg");
                assertThat(f.productCode()).isEqualTo("SKU-100");
                assertThat(f.branchName()).isEqualTo("Sede Centro");
                assertThat(f.quantity()).isEqualTo(12);
            });
        }

        @Test
        @DisplayName("marca bajo minimo comparando cantidad contra el minimo de la fila")
        void marca_bajo_minimo() {
            saldo(PRODUCT, BRANCH, 3, 5);
            saldo(OTRO_PRODUCT, BRANCH, 10, 5);

            List<StockView> filas = adapter
                    .searchStock(new SearchStockCommand(COMPANY, null, null, false, 0, 20))
                    .content();

            assertThat(filas).filteredOn(StockView::lowStock).extracting(StockView::productId)
                    .containsExactly(PRODUCT);
        }

        @Test
        @DisplayName("el filtro de bajo minimo deja fuera lo que cuadra")
        void el_filtro_de_bajo_minimo_deja_fuera_lo_que_cuadra() {
            saldo(PRODUCT, BRANCH, 3, 5);
            saldo(OTRO_PRODUCT, BRANCH, 10, 5);

            assertThat(adapter.searchStock(new SearchStockCommand(COMPANY, null, null, true, 0, 20))
                    .content()).extracting(StockView::productId).containsExactly(PRODUCT);
        }

        @Test
        @DisplayName("una sede null trae todas las sedes")
        void una_sede_null_trae_todas_las_sedes() {
            saldo(PRODUCT, BRANCH, 12, 5);
            saldo(PRODUCT, OTRA_BRANCH, 4, 5);

            // El `(:branchId IS NULL OR ...)` es lo que sostiene la vista consolidada.
            assertThat(
                    adapter.searchStock(new SearchStockCommand(COMPANY, null, null, false, 0, 20))
                            .content())
                    .hasSize(2);
            assertThat(
                    adapter.searchStock(new SearchStockCommand(COMPANY, BRANCH, null, false, 0, 20))
                            .content())
                    .hasSize(1);
        }

        @Test
        @DisplayName("el texto busca por nombre y por codigo")
        void el_texto_busca_por_nombre_y_por_codigo() {
            saldo(PRODUCT, BRANCH, 12, 5);
            saldo(OTRO_PRODUCT, BRANCH, 8, 5);

            assertThat(adapter
                    .searchStock(new SearchStockCommand(COMPANY, null, "moxicil", false, 0, 20))
                    .content()).extracting(StockView::productId).containsExactly(PRODUCT);
            assertThat(adapter
                    .searchStock(new SearchStockCommand(COMPANY, null, "SKU-200", false, 0, 20))
                    .content()).extracting(StockView::productId).containsExactly(OTRO_PRODUCT);
        }

        @Test
        @DisplayName("un texto en blanco no filtra nada")
        void un_texto_en_blanco_no_filtra_nada() {
            saldo(PRODUCT, BRANCH, 12, 5);

            // El adaptador normaliza " " a null antes de la query: si lo pasara tal
            // cual, el LIKE '% %' no encontraria nada y el listado saldria vacio.
            assertThat(
                    adapter.searchStock(new SearchStockCommand(COMPANY, null, "   ", false, 0, 20))
                            .content())
                    .hasSize(1);
        }

        @Test
        @DisplayName("un tamaño de pagina invalido cae al valor por defecto")
        void un_tamano_de_pagina_invalido_cae_al_defecto() {
            saldo(PRODUCT, BRANCH, 12, 5);

            // PageRequest revienta con size 0: el adaptador tiene que defenderse antes.
            assertThat(
                    adapter.searchStock(new SearchStockCommand(COMPANY, null, null, false, -1, 0))
                            .pageSize())
                    .isEqualTo(20);
        }

        @Test
        @DisplayName("el saldo de otra empresa no se ve")
        void el_saldo_de_otra_empresa_no_se_ve() {
            saldo(PRODUCT, BRANCH, 12, 5);

            assertThat(adapter.searchStock(
                    new SearchStockCommand(SchemaSeed.OTRA_COMPANY_ID, null, null, false, 0, 20))
                    .content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("listLots — los lotes con existencia")
    class Lotes {

        @Test
        @DisplayName("devuelve los lotes en orden FEFO y solo los que tienen existencia")
        void devuelve_los_lotes_en_orden_fefo() {
            lote(PRODUCT, BRANCH, LocalDate.of(2027, 6, 30), 5, COSTO);
            Long enero = lote(PRODUCT, BRANCH, LocalDate.of(2027, 1, 31), 3, COSTO);
            lote(PRODUCT, BRANCH, LocalDate.of(2027, 3, 31), 0, COSTO);

            List<StockLotView> lotes = adapter.listLots(COMPANY, BRANCH, PRODUCT);

            assertThat(lotes).hasSize(2);
            assertThat(lotes.getFirst().lotId()).isEqualTo(enero);
            assertThat(lotes.getFirst().quantityAvailable()).isEqualTo(3);
        }

        @Test
        @DisplayName("una sede null junta los lotes de todas las sedes")
        void una_sede_null_junta_todas_las_sedes() {
            lote(PRODUCT, BRANCH, LocalDate.of(2027, 1, 31), 3, COSTO);
            lote(PRODUCT, OTRA_BRANCH, LocalDate.of(2027, 6, 30), 5, COSTO);

            assertThat(adapter.listLots(COMPANY, null, PRODUCT)).hasSize(2);
        }
    }

    @Nested
    @DisplayName("searchKardex — el rango de fechas")
    class Kardex {

        @Test
        @DisplayName("el hasta es inclusivo por dia: incluye lo del mismo dia a cualquier hora")
        void el_hasta_es_inclusivo_por_dia() {
            Long lot = lote(PRODUCT, BRANCH, null, 10, COSTO);
            movimiento(PRODUCT, BRANCH, lot, "PURCHASE", 10, ENERO_20);

            // El adaptador convierte el `to` a exclusivo al inicio del dia siguiente. Sin
            // eso, un movimiento de las 09:00 del ultimo dia del rango se perderia y el
            // kardex del mes no cuadraria con el saldo.
            var pagina = adapter.searchKardex(new SearchKardexCommand(COMPANY, BRANCH, PRODUCT,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 20), 0, 20));

            assertThat(pagina.content()).hasSize(1);
        }

        @Test
        @DisplayName("lo que cae fuera del rango no entra")
        void lo_que_cae_fuera_del_rango_no_entra() {
            Long lot = lote(PRODUCT, BRANCH, null, 10, COSTO);
            movimiento(PRODUCT, BRANCH, lot, "PURCHASE", 10, ENERO_10);
            movimiento(PRODUCT, BRANCH, lot, "SALE", -2, FEBRERO_5);

            var pagina = adapter.searchKardex(new SearchKardexCommand(COMPANY, BRANCH, PRODUCT,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 0, 20));

            assertThat(pagina.content()).hasSize(1);
            assertThat(pagina.content().getFirst().quantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("sin rango trae todo el historico, mas reciente primero")
        void sin_rango_trae_todo_el_historico() {
            Long lot = lote(PRODUCT, BRANCH, null, 10, COSTO);
            movimiento(PRODUCT, BRANCH, lot, "PURCHASE", 10, ENERO_10);
            movimiento(PRODUCT, BRANCH, lot, "SALE", -2, FEBRERO_5);

            var pagina = adapter.searchKardex(
                    new SearchKardexCommand(COMPANY, BRANCH, PRODUCT, null, null, 0, 20));

            assertThat(pagina.content()).extracting(v -> v.quantity()).containsExactly(-2, 10);
            assertThat(pagina.totalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("solo con fecha desde: trae lo posterior, sin limite superior")
        void solo_con_fecha_desde() {
            Long lot = lote(PRODUCT, BRANCH, null, 10, COSTO);
            movimiento(PRODUCT, BRANCH, lot, "PURCHASE", 10, ENERO_10);
            movimiento(PRODUCT, BRANCH, lot, "SALE", -2, FEBRERO_5);

            var pagina = adapter.searchKardex(new SearchKardexCommand(COMPANY, BRANCH, PRODUCT,
                    LocalDate.of(2026, 1, 15), null, 0, 20));

            assertThat(pagina.content()).extracting(v -> v.quantity()).containsExactly(-2);
        }

        @Test
        @DisplayName("solo con fecha hasta: trae lo anterior, sin limite inferior")
        void solo_con_fecha_hasta() {
            Long lot = lote(PRODUCT, BRANCH, null, 10, COSTO);
            movimiento(PRODUCT, BRANCH, lot, "PURCHASE", 10, ENERO_10);
            movimiento(PRODUCT, BRANCH, lot, "SALE", -2, FEBRERO_5);

            var pagina = adapter.searchKardex(new SearchKardexCommand(COMPANY, BRANCH, PRODUCT,
                    null, LocalDate.of(2026, 1, 31), 0, 20));

            assertThat(pagina.content()).extracting(v -> v.quantity()).containsExactly(10);
        }
    }

    @Nested
    @DisplayName("openingBalance — el saldo antes del rango")
    class SaldoInicial {

        @Test
        @DisplayName("suma con signo todo lo anterior a la fecha")
        void suma_con_signo_todo_lo_anterior() {
            Long lot = lote(PRODUCT, BRANCH, null, 10, COSTO);
            movimiento(PRODUCT, BRANCH, lot, "PURCHASE", 10, ENERO_10);
            movimiento(PRODUCT, BRANCH, lot, "SALE", -4, ENERO_10);
            movimiento(PRODUCT, BRANCH, lot, "PURCHASE", 7, FEBRERO_5);

            assertThat(adapter.openingBalance(COMPANY, PRODUCT, BRANCH, LocalDate.of(2026, 2, 1)))
                    .isEqualTo(6);
        }

        @Test
        @DisplayName("sin movimientos previos devuelve cero, no null")
        void sin_movimientos_previos_devuelve_cero() {
            // El COALESCE de la query es lo que evita un NullPointerException al
            // desempaquetar el int. Un producto nuevo entra por aqui siempre.
            assertThat(adapter.openingBalance(COMPANY, PRODUCT, BRANCH, LocalDate.of(2026, 1, 1)))
                    .isZero();
        }
    }

    @Nested
    @DisplayName("alerts — bajo minimo y por vencer")
    class Alertas {

        @Test
        @DisplayName("lista lo que esta bajo minimo")
        void lista_lo_que_esta_bajo_minimo() {
            saldo(PRODUCT, BRANCH, 3, 5);
            saldo(OTRO_PRODUCT, BRANCH, 10, 5);

            InventoryAlertsView alertas = adapter.alerts(COMPANY, null, 30);

            assertThat(alertas.lowStock()).extracting(StockView::productId)
                    .containsExactly(PRODUCT);
        }

        @Test
        @DisplayName("los dias que faltan los calcula la base, con los nombres ya resueltos")
        void los_dias_que_faltan_los_calcula_la_base() {
            lote(PRODUCT, BRANCH, LocalDate.now().plusDays(10), 5, COSTO);

            InventoryAlertsView alertas = adapter.alerts(COMPANY, null, 30);

            // La cuenta sale de un DATEDIFF contra el CURRENT_DATE del SERVIDOR, mientras
            // que el limite de la ventana lo calcula la JVM con LocalDate.now(). Si los
            // dos relojes estan en husos distintos —el contenedor va en UTC y esta JVM en
            // Bogota— el numero baila un dia. Por eso la asercion es una ventana: fijar
            // el 10 exacto haria que este test fallara solo, de noche, en un huso y no en
            // otro. Ver la nota sobre el desfase en el comentario de la clase.
            assertThat(alertas.expiring()).singleElement().satisfies(l -> {
                assertThat(l.daysToExpire()).isBetween(9L, 10L);
                assertThat(l.productName()).isEqualTo("Amoxicilina 500mg");
                assertThat(l.branchName()).isEqualTo("Sede Centro");
                assertThat(l.quantityAvailable()).isEqualTo(5);
            });
        }

        @Test
        @DisplayName("un lote ya vencido entra con dias negativos")
        void un_lote_ya_vencido_entra_con_dias_negativos() {
            lote(PRODUCT, BRANCH, LocalDate.now().minusDays(3), 5, COSTO);

            // Lo vencido es justo lo que hay que sacar de la estanteria: si la ventana lo
            // dejara fuera por ser anterior a hoy, la alerta seria inutil. El signo es lo
            // que importa; la magnitud exacta depende del huso del servidor.
            assertThat(adapter.alerts(COMPANY, null, 30).expiring()).singleElement()
                    .satisfies(l -> assertThat(l.daysToExpire()).isNegative());
        }

        @Test
        @DisplayName("un lote que vence despues de la ventana no alerta")
        void un_lote_fuera_de_la_ventana_no_alerta() {
            lote(PRODUCT, BRANCH, LocalDate.now().plusDays(90), 5, COSTO);

            assertThat(adapter.alerts(COMPANY, null, 30).expiring()).isEmpty();
        }

        @Test
        @DisplayName("un lote sin vencimiento nunca alerta")
        void un_lote_sin_vencimiento_nunca_alerta() {
            lote(PRODUCT, BRANCH, null, 5, COSTO);

            assertThat(adapter.alerts(COMPANY, null, 3650).expiring()).isEmpty();
        }

        @Test
        @DisplayName("un numero de dias negativo no agranda la ventana hacia atras")
        void dias_negativos_no_agranda_la_ventana() {
            lote(PRODUCT, BRANCH, LocalDate.now().plusDays(5), 5, COSTO);

            // Math.max(0, expiringInDays) es lo que evita que un negativo mueva el
            // limite antes de hoy: con -10 el limite sigue siendo HOY, y un lote que
            // vence en 5 dias queda fuera de la ventana.
            assertThat(adapter.alerts(COMPANY, null, -10).expiring()).isEmpty();
        }
    }

    @Nested
    @DisplayName("valuation — cuanto vale lo que hay")
    class Valuacion {

        @Test
        @DisplayName("suma unidades por costo y agrupa por producto")
        void suma_unidades_por_costo_y_agrupa_por_producto() {
            lote(PRODUCT, BRANCH, null, 4, new BigDecimal("1000.0000"));
            lote(PRODUCT, BRANCH, null, 6, new BigDecimal("1500.0000"));
            lote(OTRO_PRODUCT, BRANCH, null, 2, new BigDecimal("500.0000"));

            InventoryValuationView valuacion = adapter.valuation(COMPANY, null);

            // 4x1000 + 6x1500 = 13.000 del primero, 2x500 = 1.000 del segundo.
            assertThat(valuacion.byProduct()).hasSize(2);
            assertThat(valuacion.totalValue()).isEqualByComparingTo("14000");
            assertThat(valuacion.totalUnits()).isEqualTo(12);
        }

        @Test
        @DisplayName("un producto cuyos lotes suman cero no ensucia el reporte")
        void un_producto_que_suma_cero_no_ensucia_el_reporte() {
            lote(PRODUCT, BRANCH, null, 5, COSTO);
            lote(PRODUCT, BRANCH, null, -5, COSTO);

            // El HAVING de la query: un producto que entro y salio entero no aporta valor
            // y no tiene por que aparecer en la valuacion.
            assertThat(adapter.valuation(COMPANY, null).byProduct()).isEmpty();
        }

        @Test
        @DisplayName("sin inventario el total es cero, no null")
        void sin_inventario_el_total_es_cero() {
            InventoryValuationView valuacion = adapter.valuation(COMPANY, null);

            assertThat(valuacion.totalValue()).isEqualByComparingTo("0");
            assertThat(valuacion.totalUnits()).isZero();
        }
    }

    @Nested
    @DisplayName("purchases — el libro de compras")
    class Compras {

        @Test
        @DisplayName("solo trae entradas de compra, con su total calculado")
        void solo_trae_entradas_de_compra() {
            Long lot = lote(PRODUCT, BRANCH, null, 10, COSTO);
            movimiento(PRODUCT, BRANCH, lot, "PURCHASE", 10, ENERO_10);
            movimiento(PRODUCT, BRANCH, lot, "SALE", -2, ENERO_10);

            List<PurchaseView> compras = adapter
                    .purchases(new SearchPurchasesQuery(COMPANY, null, null, null, 0, 20))
                    .content();

            assertThat(compras).singleElement().satisfies(c -> {
                assertThat(c.quantity()).isEqualTo(10);
                assertThat(c.total()).isEqualByComparingTo("15000");
                assertThat(c.productName()).isEqualTo("Amoxicilina 500mg");
            });
        }

        @Test
        @DisplayName("la version de exportacion trae las mismas filas sin paginar")
        void la_version_de_exportacion_trae_las_mismas_filas() {
            Long lot = lote(PRODUCT, BRANCH, null, 10, COSTO);
            movimiento(PRODUCT, BRANCH, lot, "PURCHASE", 10, ENERO_10);
            movimiento(PRODUCT, BRANCH, lot, "PURCHASE", 4, FEBRERO_5);

            assertThat(adapter
                    .purchasesForExport(new SearchPurchasesQuery(COMPANY, null, null, null, 0, 1)))
                    .hasSize(2);
        }

        @Test
        @DisplayName("un rango de fechas deja fuera las compras anteriores o posteriores")
        void un_rango_de_fechas_filtra_las_compras_fuera_de_el() {
            Long lot = lote(PRODUCT, BRANCH, null, 10, COSTO);
            movimiento(PRODUCT, BRANCH, lot, "PURCHASE", 10, ENERO_10);
            movimiento(PRODUCT, BRANCH, lot, "PURCHASE", 4, FEBRERO_5);

            List<PurchaseView> compras = adapter.purchases(new SearchPurchasesQuery(COMPANY, null,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 0, 20)).content();

            assertThat(compras).singleElement()
                    .satisfies(c -> assertThat(c.quantity()).isEqualTo(10));
        }

        @Test
        @DisplayName("la version de exportacion tambien respeta el rango de fechas")
        void la_version_de_exportacion_respeta_el_rango() {
            Long lot = lote(PRODUCT, BRANCH, null, 10, COSTO);
            movimiento(PRODUCT, BRANCH, lot, "PURCHASE", 10, ENERO_10);
            movimiento(PRODUCT, BRANCH, lot, "PURCHASE", 4, FEBRERO_5);

            assertThat(adapter.purchasesForExport(new SearchPurchasesQuery(COMPANY, null,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 0, 20))).hasSize(1);
        }
    }

    @Nested
    @DisplayName("kardexForExport — el kardex sin paginar")
    class KardexExportado {

        @Test
        @DisplayName("viene en orden ascendente, que es como se arrastra el saldo corrido")
        void viene_en_orden_ascendente() {
            Long lot = lote(PRODUCT, BRANCH, null, 10, COSTO);
            movimiento(PRODUCT, BRANCH, lot, "PURCHASE", 10, ENERO_10);
            movimiento(PRODUCT, BRANCH, lot, "SALE", -2, FEBRERO_5);

            // Al reves que searchKardex, que va descendente para la pantalla: el reporte
            // necesita el orden cronologico o el saldo corrido sale al reves.
            List<KardexExportRow> filas = adapter.kardexForExport(
                    new SearchKardexCommand(COMPANY, BRANCH, PRODUCT, null, null, 0, 0));

            assertThat(filas).extracting(KardexExportRow::quantity).containsExactly(10, -2);
            assertThat(filas.getFirst().productName()).isEqualTo("Amoxicilina 500mg");
            assertThat(filas.getFirst().branchName()).isEqualTo("Sede Centro");
        }

        @Test
        @DisplayName("respeta el rango de fechas igual que la version paginada")
        void respeta_el_rango_de_fechas() {
            Long lot = lote(PRODUCT, BRANCH, null, 10, COSTO);
            movimiento(PRODUCT, BRANCH, lot, "PURCHASE", 10, ENERO_10);
            movimiento(PRODUCT, BRANCH, lot, "SALE", -2, FEBRERO_5);

            List<KardexExportRow> filas = adapter.kardexForExport(new SearchKardexCommand(COMPANY,
                    BRANCH, PRODUCT, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 0, 0));

            assertThat(filas).extracting(KardexExportRow::quantity).containsExactly(10);
        }
    }
}
