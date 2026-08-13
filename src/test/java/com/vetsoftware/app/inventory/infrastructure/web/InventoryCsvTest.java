package com.vetsoftware.app.inventory.infrastructure.web;

import static com.vetsoftware.app.inventory.testsupport.InventoryMother.CREADO;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.LOT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.compra;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.inventory.application.dto.KardexReport;
import com.vetsoftware.app.inventory.application.dto.KardexReportLine;
import com.vetsoftware.app.inventory.application.dto.PurchaseView;
import com.vetsoftware.app.inventory.application.dto.PurchasesReport;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El CSV es lo que el contador abre en Excel: si el formato se rompe, el kardex
 * deja de cuadrar en una hoja que nadie va a depurar leyendo Java.
 */
@DisplayName("InventoryCsv — serializacion de los reportes de inventario")
class InventoryCsvTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 1, 31);

    private static KardexReport kardex(List<KardexReportLine> lineas, int inicial, int fina) {
        return new KardexReport("Amoxicilina 500mg", "SKU-100", "Sede Centro", DESDE, HASTA,
                LocalDateTime.now(), inicial, fina, lineas);
    }

    private static KardexReportLine linea(String tipo, String referencia, Long lotId, int cantidad,
            String costo, int saldo) {
        return new KardexReportLine(CREADO, tipo, referencia, lotId, cantidad,
                costo == null ? null : new BigDecimal(costo), saldo);
    }

    private static String csvKardex(KardexReport reporte) {
        return new String(InventoryCsv.kardex(reporte), StandardCharsets.UTF_8);
    }

    private static String csvCompras(PurchasesReport reporte) {
        return new String(InventoryCsv.purchases(reporte), StandardCharsets.UTF_8);
    }

    private static PurchasesReport compras(List<PurchaseView> lineas, int unidades, String total) {
        return new PurchasesReport("Sede Centro", DESDE, HASTA, LocalDateTime.now(), lineas,
                unidades, new BigDecimal(total));
    }

    @Nested
    @DisplayName("formato del archivo")
    class Formato {

        @Test
        @DisplayName("el kardex arranca con el BOM UTF-8 para que Excel respete los acentos")
        void el_kardex_arranca_con_el_bom() {
            byte[] csv = InventoryCsv.kardex(kardex(List.of(), 0, 0));

            // Sin BOM, Excel abre el archivo en la codificacion del sistema y "Recepción"
            // sale como "RecepciÃ³n". Es el bug clasico de los CSV en Windows.
            assertThat(csv[0]).isEqualTo((byte) 0xEF);
            assertThat(csv[1]).isEqualTo((byte) 0xBB);
            assertThat(csv[2]).isEqualTo((byte) 0xBF);
        }

        @Test
        @DisplayName("el libro de compras tambien lleva BOM")
        void el_libro_de_compras_tambien_lleva_bom() {
            assertThat(csvCompras(compras(List.of(), 0, "0"))).startsWith("﻿Fecha,Producto");
        }

        @Test
        @DisplayName("separa las filas con CRLF, como manda el RFC 4180")
        void separa_las_filas_con_crlf() {
            assertThat(csvKardex(kardex(List.of(), 0, 0))).contains("\r\n");
        }
    }

    @Nested
    @DisplayName("kardex")
    class Kardex {

        @Test
        @DisplayName("abre y cierra el listado con el saldo inicial y el final")
        void abre_y_cierra_con_los_saldos() {
            String csv = csvKardex(kardex(List.of(), 20, 26));

            // Las dos filas de contexto son lo que hace auditable un rango de fechas: sin
            // ellas, el lector no sabe de donde salio el primer saldo corrido.
            assertThat(csv).contains(",Saldo inicial,,,,,20\r\n")
                    .contains(",Saldo final,,,,,26\r\n");
        }

        @Test
        @DisplayName("escribe la cabecera de columnas del kardex")
        void escribe_la_cabecera_de_columnas() {
            assertThat(csvKardex(kardex(List.of(), 0, 0)))
                    .contains("Fecha,Tipo,Referencia,Lote,Cantidad,Costo unitario,Saldo\r\n");
        }

        @Test
        @DisplayName("escribe cada movimiento con su fecha legible, cantidad y saldo corrido")
        void escribe_cada_movimiento() {
            String csv = csvKardex(kardex(
                    List.of(linea("Compra", "Recepción #900", LOT_ID, 10, "1500", 30)), 20, 30));

            assertThat(csv).contains("15/01/2026 10:30,Compra,Recepción #900,700,10,1500,30\r\n");
        }

        @Test
        @DisplayName("una salida conserva su signo negativo")
        void una_salida_conserva_su_signo() {
            String csv = csvKardex(kardex(
                    List.of(linea("Venta", "Venta POS #901", LOT_ID, -4, "1500", 26)), 30, 26));

            // La columna se suma tal cual en la hoja y tiene que dar el movimiento neto.
            assertThat(csv).contains(",Venta,Venta POS #901,700,-4,1500,26\r\n");
        }

        @Test
        @DisplayName("un movimiento sin lote deja la celda vacia")
        void un_movimiento_sin_lote_deja_la_celda_vacia() {
            String csv = csvKardex(kardex(
                    List.of(linea("Ajuste entrada", "Ajuste #500", null, 3, "0", 33)), 30, 33));

            assertThat(csv).contains(",Ajuste entrada,Ajuste #500,,3,0,33\r\n");
        }

        @Test
        @DisplayName("un costo null se escribe como cero, no como celda vacia")
        void un_costo_null_se_escribe_como_cero() {
            String csv = csvKardex(kardex(
                    List.of(linea("Ajuste salida", "Ajuste #500", LOT_ID, -2, null, 28)), 30, 28));

            // Cero y no vacio: la columna de costo se suma, y una celda vacia entre
            // numeros rompe la formula en Excel.
            assertThat(csv).contains(",Ajuste salida,Ajuste #500,700,-2,0,28\r\n");
        }

        @Test
        @DisplayName("los ceros de mas del costo se recortan")
        void los_ceros_de_mas_del_costo_se_recortan() {
            String csv = csvKardex(kardex(
                    List.of(linea("Compra", "Recepción #900", LOT_ID, 10, "1500.00", 30)), 20, 30));

            assertThat(csv).contains(",10,1500,30\r\n");
        }

        @Test
        @DisplayName("una referencia con coma se entrecomilla para no partir la fila")
        void una_referencia_con_coma_se_entrecomilla() {
            String csv = csvKardex(kardex(
                    List.of(linea("Ajuste entrada", "Ajuste, conteo ciclico", LOT_ID, 3, "0", 33)),
                    30, 33));

            assertThat(csv).contains("\"Ajuste, conteo ciclico\"");
        }

        @Test
        @DisplayName("una referencia con comillas las duplica")
        void una_referencia_con_comillas_las_duplica() {
            String csv = csvKardex(kardex(
                    List.of(linea("Ajuste entrada", "Ajuste \"urgente\"", LOT_ID, 3, "0", 33)), 30,
                    33));

            assertThat(csv).contains("\"Ajuste \"\"urgente\"\"\"");
        }
    }

    @Nested
    @DisplayName("libro de compras")
    class Compras {

        @Test
        @DisplayName("escribe la cabecera de columnas del libro")
        void escribe_la_cabecera_de_columnas() {
            assertThat(csvCompras(compras(List.of(), 0, "0")))
                    .contains("Fecha,Producto,SKU,Sede,Lote,Cantidad,Costo unitario,Total\r\n");
        }

        @Test
        @DisplayName("escribe cada entrada con su producto, sede y costo")
        void escribe_cada_entrada() {
            String csv = csvCompras(compras(List.of(compra(1L, 10, "15000")), 10, "15000"));

            assertThat(csv).contains(
                    "15/01/2026 10:30,Amoxicilina 500mg,SKU-100,Sede Centro,700,10,1500,15000\r\n");
        }

        @Test
        @DisplayName("una cantidad guardada con signo se escribe en valor absoluto")
        void una_cantidad_con_signo_se_escribe_en_absoluto() {
            String csv = csvCompras(compras(List.of(compra(1L, -10, "15000")), 10, "15000"));

            // Todas las filas del libro son entradas: enseñar −10 confundiria al lector
            // y descuadraria la suma de la columna contra el total.
            assertThat(csv).contains(",700,10,1500,15000\r\n");
        }

        @Test
        @DisplayName("cierra con la fila TOTAL de unidades y valor")
        void cierra_con_la_fila_total() {
            String csv = csvCompras(
                    compras(List.of(compra(1L, 10, "15000"), compra(2L, 4, "6000")), 14, "21000"));

            assertThat(csv).contains(",TOTAL,,,,14,,21000\r\n");
        }

        @Test
        @DisplayName("un rango sin compras deja solo cabecera y total en cero")
        void un_rango_sin_compras_deja_cabecera_y_total() {
            String csv = csvCompras(compras(List.of(), 0, "0"));

            assertThat(csv).contains(",TOTAL,,,,0,,0\r\n");
        }
    }
}
