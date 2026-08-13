package com.vetsoftware.app.inventory.application.usecase;

import static com.vetsoftware.app.inventory.testsupport.InventoryMother.BRANCH_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.COMPANY_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.PRODUCT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.compra;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.filaKardex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.inventory.application.command.SearchKardexCommand;
import com.vetsoftware.app.inventory.application.command.SearchPurchasesQuery;
import com.vetsoftware.app.inventory.application.dto.KardexReport;
import com.vetsoftware.app.inventory.application.dto.KardexReportLine;
import com.vetsoftware.app.inventory.application.dto.PurchasesReport;
import com.vetsoftware.app.inventory.application.port.out.StockQueryPort;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryReportService — kardex con saldo corrido y libro de compras")
class InventoryReportServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 1, 31);

    @Mock
    private StockQueryPort stockQueryPort;

    @InjectMocks
    private InventoryReportService service;

    private static SearchKardexCommand kardexDe(Long branchId, LocalDate desde) {
        return new SearchKardexCommand(COMPANY_ID, branchId, PRODUCT_ID, desde, HASTA, 0, 0);
    }

    private static SearchPurchasesQuery comprasDe(Long branchId) {
        return new SearchPurchasesQuery(COMPANY_ID, branchId, DESDE, HASTA, 0, 0);
    }

    @Nested
    @DisplayName("kardex: el saldo corrido")
    class SaldoCorrido {

        @Test
        @DisplayName("arrastra el saldo movimiento a movimiento")
        void arrastra_el_saldo_movimiento_a_movimiento() {
            when(stockQueryPort.openingBalance(COMPANY_ID, PRODUCT_ID, BRANCH_ID, DESDE))
                    .thenReturn(20);
            when(stockQueryPort.kardexForExport(any()))
                    .thenReturn(List.of(filaKardex("PURCHASE", "GOODS_RECEIPT", 900L, 10),
                            filaKardex("SALE", "POS_DOCUMENT", 901L, -4),
                            filaKardex("ADJUSTMENT_OUT", "ADJUSTMENT", 902L, -6)));

            KardexReport reporte = service.kardexReport(kardexDe(BRANCH_ID, DESDE));

            // 20 inicial, +10, −4, −6 → 20, 30, 26, 20.
            assertThat(reporte.openingBalance()).isEqualTo(20);
            assertThat(reporte.lines()).extracting(KardexReportLine::runningBalance)
                    .containsExactly(30, 26, 20);
            assertThat(reporte.closingBalance()).isEqualTo(20);
        }

        @Test
        @DisplayName("sin filtro de fecha el saldo inicial es cero y no se consulta")
        void sin_filtro_de_fecha_el_saldo_inicial_es_cero() {
            when(stockQueryPort.kardexForExport(any()))
                    .thenReturn(List.of(filaKardex("PURCHASE", "GOODS_RECEIPT", 900L, 10)));

            KardexReport reporte = service.kardexReport(kardexDe(BRANCH_ID, null));

            // Sin rango, el kardex viene desde el principio: pedir el saldo previo seria
            // una query de mas que siempre daria cero.
            assertThat(reporte.openingBalance()).isZero();
            assertThat(reporte.closingBalance()).isEqualTo(10);
            verify(stockQueryPort, never()).openingBalance(any(), any(), any(), any());
        }

        @Test
        @DisplayName("un kardex vacio conserva el saldo inicial como final")
        void un_kardex_vacio_conserva_el_saldo_inicial() {
            when(stockQueryPort.openingBalance(COMPANY_ID, PRODUCT_ID, BRANCH_ID, DESDE))
                    .thenReturn(20);
            when(stockQueryPort.kardexForExport(any())).thenReturn(List.of());

            KardexReport reporte = service.kardexReport(kardexDe(BRANCH_ID, DESDE));

            // Un mes sin movimientos no significa saldo cero: significa que no se movio.
            assertThat(reporte.openingBalance()).isEqualTo(20);
            assertThat(reporte.closingBalance()).isEqualTo(20);
            assertThat(reporte.lines()).isEmpty();
        }

        @Test
        @DisplayName("un saldo inicial negativo se arrastra igual")
        void un_saldo_inicial_negativo_se_arrastra_igual() {
            when(stockQueryPort.openingBalance(COMPANY_ID, PRODUCT_ID, BRANCH_ID, DESDE))
                    .thenReturn(-5);
            when(stockQueryPort.kardexForExport(any()))
                    .thenReturn(List.of(filaKardex("PURCHASE", "GOODS_RECEIPT", 900L, 8)));

            // Si la empresa permite stock negativo, el reporte tiene que poder enseñarlo.
            assertThat(service.kardexReport(kardexDe(BRANCH_ID, DESDE)).closingBalance())
                    .isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("kardex: cabecera")
    class Cabecera {

        @Test
        @DisplayName("toma el nombre del producto y de la sede de la primera fila")
        void toma_los_nombres_de_la_primera_fila() {
            when(stockQueryPort.kardexForExport(any()))
                    .thenReturn(List.of(filaKardex("PURCHASE", "GOODS_RECEIPT", 900L, 10)));

            KardexReport reporte = service.kardexReport(kardexDe(BRANCH_ID, null));

            assertThat(reporte.productName()).isEqualTo("Amoxicilina 500mg");
            assertThat(reporte.productCode()).isEqualTo("SKU-100");
            assertThat(reporte.branchName()).isEqualTo("Sede Centro");
            assertThat(reporte.toDate()).isEqualTo(HASTA);
            assertThat(reporte.generatedAt()).isNotNull();
        }

        @Test
        @DisplayName("sin sede en el filtro la cabecera dice que son todas")
        void sin_sede_la_cabecera_dice_que_son_todas() {
            when(stockQueryPort.kardexForExport(any()))
                    .thenReturn(List.of(filaKardex("PURCHASE", "GOODS_RECEIPT", 900L, 10)));

            // El reporte consolidado no puede rotularse con la sede de la primera fila:
            // seria mentir sobre el alcance de los numeros que enseña.
            assertThat(service.kardexReport(kardexDe(null, null)).branchName())
                    .isEqualTo("Todas las sedes");
        }

        @Test
        @DisplayName("un kardex vacio deja los nombres en null en vez de reventar")
        void un_kardex_vacio_deja_los_nombres_en_null() {
            when(stockQueryPort.kardexForExport(any())).thenReturn(List.of());

            KardexReport reporte = service.kardexReport(kardexDe(BRANCH_ID, null));

            assertThat(reporte.productName()).isNull();
            assertThat(reporte.productCode()).isNull();
            assertThat(reporte.branchName()).isNull();
        }
    }

    @Nested
    @DisplayName("kardex: etiquetas en castellano")
    class Etiquetas {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({"PURCHASE, Compra", "SALE, Venta", "ADJUSTMENT_IN, Ajuste entrada",
                "ADJUSTMENT_OUT, Ajuste salida", "CLINICAL_USE, Consumo clínico",
                "TRANSFER_OUT, Transferencia salida", "TRANSFER_IN, Transferencia entrada",
                "VOID_IN, Reversa entrada", "VOID_OUT, Reversa salida"})
        @DisplayName("cada tipo de movimiento tiene su etiqueta")
        void cada_tipo_tiene_su_etiqueta(String tipo, String etiqueta) {
            when(stockQueryPort.kardexForExport(any()))
                    .thenReturn(List.of(filaKardex(tipo, "ADJUSTMENT", 900L, 1)));

            assertThat(
                    service.kardexReport(kardexDe(BRANCH_ID, null)).lines().getFirst().typeLabel())
                    .isEqualTo(etiqueta);
        }

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({"POS_DOCUMENT, Venta POS #900", "OPEN_ACCOUNT_CHARGE, Cuenta abierta #900",
                "GOODS_RECEIPT, Recepción #900", "ADJUSTMENT, Ajuste #900",
                "TRANSFER, Transferencia #900", "CLINICAL_EVENT, Evento clínico #900"})
        @DisplayName("cada origen se traduce y arrastra su numero de documento")
        void cada_origen_se_traduce_con_su_numero(String origen, String etiqueta) {
            when(stockQueryPort.kardexForExport(any()))
                    .thenReturn(List.of(filaKardex("PURCHASE", origen, 900L, 1)));

            assertThat(service.kardexReport(kardexDe(BRANCH_ID, null)).lines().getFirst()
                    .referenceLabel()).isEqualTo(etiqueta);
        }

        @Test
        @DisplayName("un origen sin numero se queda solo con la etiqueta")
        void un_origen_sin_numero_se_queda_con_la_etiqueta() {
            when(stockQueryPort.kardexForExport(any()))
                    .thenReturn(List.of(filaKardex("PURCHASE", "ADJUSTMENT", null, 1)));

            assertThat(service.kardexReport(kardexDe(BRANCH_ID, null)).lines().getFirst()
                    .referenceLabel()).isEqualTo("Ajuste");
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("un tipo null sale como celda vacia, no revienta el reporte")
        void un_tipo_null_sale_como_celda_vacia(String tipo) {
            when(stockQueryPort.kardexForExport(any()))
                    .thenReturn(List.of(filaKardex(tipo, null, null, 1)));

            KardexReportLine linea = service.kardexReport(kardexDe(BRANCH_ID, null)).lines()
                    .getFirst();

            assertThat(linea.typeLabel()).isEmpty();
            assertThat(linea.referenceLabel()).isEmpty();
        }

        @Test
        @DisplayName("un tipo desconocido se enseña tal cual en vez de perderse")
        void un_tipo_desconocido_se_muestra_tal_cual() {
            when(stockQueryPort.kardexForExport(any()))
                    .thenReturn(List.of(filaKardex("TIPO_NUEVO", "ORIGEN_NUEVO", 900L, 1)));

            // El dia que se añada un tipo al enum y se olvide aqui, el reporte enseña el
            // codigo crudo: feo pero honesto, mejor que una celda vacia.
            KardexReportLine linea = service.kardexReport(kardexDe(BRANCH_ID, null)).lines()
                    .getFirst();
            assertThat(linea.typeLabel()).isEqualTo("TIPO_NUEVO");
            assertThat(linea.referenceLabel()).isEqualTo("ORIGEN_NUEVO #900");
        }
    }

    @Nested
    @DisplayName("libro de compras")
    class LibroDeCompras {

        @Test
        @DisplayName("suma unidades y valor de todas las entradas")
        void suma_unidades_y_valor_de_todas_las_entradas() {
            when(stockQueryPort.purchasesForExport(any()))
                    .thenReturn(List.of(compra(1L, 10, "15000"), compra(2L, 4, "6000")));

            PurchasesReport reporte = service.purchasesReport(comprasDe(BRANCH_ID));

            assertThat(reporte.totalQuantity()).isEqualTo(14);
            assertThat(reporte.totalValue()).isEqualByComparingTo("21000");
            assertThat(reporte.lines()).hasSize(2);
        }

        @Test
        @DisplayName("una entrada guardada con signo negativo suma en valor absoluto")
        void una_entrada_con_signo_negativo_suma_en_valor_absoluto() {
            when(stockQueryPort.purchasesForExport(any()))
                    .thenReturn(List.of(compra(1L, -10, "15000")));

            // En el kardex las cantidades llevan signo; en el libro de compras todas las
            // filas son entradas, asi que el total se cuenta en unidades, no con signo.
            assertThat(service.purchasesReport(comprasDe(BRANCH_ID)).totalQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("un rango sin compras da totales en cero, no null")
        void un_rango_sin_compras_da_totales_en_cero() {
            when(stockQueryPort.purchasesForExport(any())).thenReturn(List.of());

            PurchasesReport reporte = service.purchasesReport(comprasDe(BRANCH_ID));

            assertThat(reporte.totalQuantity()).isZero();
            assertThat(reporte.totalValue()).isEqualByComparingTo("0");
            assertThat(reporte.branchName()).isNull();
        }

        @Test
        @DisplayName("sin sede en el filtro la cabecera dice que son todas")
        void sin_sede_la_cabecera_dice_que_son_todas() {
            when(stockQueryPort.purchasesForExport(any()))
                    .thenReturn(List.of(compra(1L, 10, "15000")));

            PurchasesReport reporte = service.purchasesReport(comprasDe(null));

            assertThat(reporte.branchName()).isEqualTo("Todas las sedes");
            assertThat(reporte.fromDate()).isEqualTo(DESDE);
            assertThat(reporte.toDate()).isEqualTo(HASTA);
        }
    }
}
