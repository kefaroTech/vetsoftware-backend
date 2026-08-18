package com.vetsoftware.app.purchasereport.infrastructure.web;

import static com.vetsoftware.app.purchasereport.testsupport.PurchaseReportMother.entradaConMontosNulos;
import static com.vetsoftware.app.purchasereport.testsupport.PurchaseReportMother.entradaConProveedor;
import static com.vetsoftware.app.purchasereport.testsupport.PurchaseReportMother.entradaSinFechas;
import static com.vetsoftware.app.purchasereport.testsupport.PurchaseReportMother.entradaSinNit;
import static com.vetsoftware.app.purchasereport.testsupport.PurchaseReportMother.libro;
import static com.vetsoftware.app.purchasereport.testsupport.PurchaseReportMother.libroConEntrada;
import static com.vetsoftware.app.purchasereport.testsupport.PurchaseReportMother.libroSinCompras;
import static com.vetsoftware.app.purchasereport.testsupport.PurchaseReportMother.libroSinRango;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.purchasereport.application.dto.PurchaseBookDto;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El CSV es lo que el contador abre en Excel: si el formato o el escapado se
 * rompen, el libro de compras deja de cuadrar en una hoja que nadie va a
 * depurar leyendo Java.
 */
@DisplayName("PurchaseBookCsv — serializacion del libro de compras a CSV")
class PurchaseBookCsvTest {

    private static String csvDe(PurchaseBookDto libro) {
        return new String(PurchaseBookCsv.purchaseBook(libro), StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("formato del archivo")
    class Formato {

        @Test
        @DisplayName("arranca con el BOM UTF-8 para que Excel respete los acentos")
        void arranca_con_el_bom_utf8() {
            byte[] csv = PurchaseBookCsv.purchaseBook(libro());

            // Sin BOM, Excel abre el archivo en la codificacion del sistema y "Retención"
            // sale ilegible. Es el bug clasico de los CSV en Windows.
            assertThat(csv[0]).isEqualTo((byte) 0xEF);
            assertThat(csv[1]).isEqualTo((byte) 0xBB);
            assertThat(csv[2]).isEqualTo((byte) 0xBF);
        }

        @Test
        @DisplayName("separa las filas con CRLF, como manda el RFC 4180")
        void separa_las_filas_con_crlf() {
            assertThat(csvDe(libro())).contains("\r\n").startsWith("﻿Libro de compras");
        }

        @Test
        @DisplayName("lleva la cabecera de columnas de la factura")
        void lleva_la_cabecera_de_columnas() {
            assertThat(csvDe(libro())).contains(
                    "Proveedor,NIT,Factura,Fecha,Vence,Base,Impuesto,Retención,Total,Pagado,Saldo,Estado");
        }
    }

    @Nested
    @DisplayName("rango de fechas")
    class RangoDeFechas {

        @Test
        @DisplayName("escribe el rango en formato legible, no ISO")
        void escribe_el_rango_en_formato_legible() {
            assertThat(csvDe(libro())).contains("Desde,01/01/2026,Hasta,31/01/2026\r\n");
        }

        @Test
        @DisplayName("un rango sin fechas deja las celdas de Desde/Hasta en blanco")
        void un_rango_sin_fechas_deja_las_celdas_en_blanco() {
            assertThat(csvDe(libroSinRango())).contains("Desde,,Hasta,\r\n");
        }
    }

    @Nested
    @DisplayName("filas de factura")
    class FilasDeFactura {

        @Test
        @DisplayName("una factura completa vuelca todas sus columnas")
        void una_factura_completa_vuelca_todas_sus_columnas() {
            assertThat(csvDe(libro())).contains(
                    "Distribuidora Sur,900123456,FC-100,10/01/2026,10/02/2026,100000,19000,0,119000,50000,69000,PARCIAL\r\n");
        }

        @Test
        @DisplayName("sin fecha de emision o vencimiento las celdas quedan en blanco")
        void sin_fecha_de_emision_o_vencimiento_las_celdas_quedan_en_blanco() {
            assertThat(csvDe(libroConEntrada(entradaSinFechas()))).contains(
                    "Distribuidora Sur,900123456,FC-100,,,100000,19000,0,119000,50000,69000,PARCIAL\r\n");
        }

        @Test
        @DisplayName("un NIT ausente sale como celda vacia, no como 'null'")
        void un_nit_ausente_sale_como_celda_vacia() {
            assertThat(csvDe(libroConEntrada(entradaSinNit())))
                    .contains("Distribuidora Sur,,FC-100,10/01/2026,10/02/2026");
        }

        @Test
        @DisplayName("un proveedor nulo no revienta la fila: la celda queda vacia")
        void un_proveedor_nulo_no_revienta_la_fila() {
            assertThat(csvDe(libroConEntrada(entradaConProveedor(null))))
                    .contains(",900123456,FC-100,10/01/2026,10/02/2026");
        }

        @Test
        @DisplayName("un periodo sin compras solo deja la fila TOTAL, sin filas de factura")
        void un_periodo_sin_compras_solo_deja_la_fila_total() {
            assertThat(csvDe(libroSinCompras())).doesNotContain("FC-100")
                    .contains("TOTAL (0),,,,,0,0,0,0,0,0,\r\n");
        }
    }

    @Nested
    @DisplayName("montos")
    class Montos {

        @Test
        @DisplayName("los ceros de mas se recortan: 100000.00 sale como 100000")
        void los_ceros_de_mas_se_recortan() {
            assertThat(csvDe(libro())).contains(",100000,19000,0,119000,50000,69000,");
        }

        @Test
        @DisplayName("montos ausentes en la entrada se muestran como cero, no como 'null'")
        void montos_ausentes_se_muestran_como_cero() {
            assertThat(csvDe(libroConEntrada(entradaConMontosNulos()))).contains(
                    "Distribuidora Sur,900123456,FC-100,10/01/2026,10/02/2026,0,0,0,0,0,0,PARCIAL\r\n");
        }
    }

    @Nested
    @DisplayName("fila TOTAL")
    class FilaTotal {

        @Test
        @DisplayName("resume la cantidad de facturas y los montos del periodo")
        void resume_la_cantidad_de_facturas_y_los_montos() {
            assertThat(csvDe(libro()))
                    .contains("TOTAL (1),,,,,100000,19000,0,119000,50000,69000,\r\n");
        }
    }

    @Nested
    @DisplayName("escapado RFC 4180")
    class Escapado {

        @Test
        @DisplayName("un proveedor con coma se entrecomilla para no partir la fila")
        void un_proveedor_con_coma_se_entrecomilla() {
            String csv = csvDe(
                    libroConEntrada(entradaConProveedor("Distribuidora Norte, Sede Centro")));

            // Sin comillas, la coma del nombre crearia una columna de mas y desplazaria
            // todo lo que viene detras (el NIT, la factura, los montos...).
            assertThat(csv).contains("\"Distribuidora Norte, Sede Centro\",900123456");
        }

        @Test
        @DisplayName("un proveedor con comillas las duplica")
        void un_proveedor_con_comillas_las_duplica() {
            String csv = csvDe(libroConEntrada(entradaConProveedor("Proveedor \"Premium\"")));

            assertThat(csv).contains("\"Proveedor \"\"Premium\"\"\",900123456");
        }

        @Test
        @DisplayName("un proveedor con salto de linea se entrecomilla")
        void un_proveedor_con_salto_de_linea_se_entrecomilla() {
            String csv = csvDe(
                    libroConEntrada(entradaConProveedor("Primera linea\nSegunda linea")));

            assertThat(csv).contains("\"Primera linea\nSegunda linea\",900123456");
        }

        @Test
        @DisplayName("un proveedor con retorno de carro se entrecomilla")
        void un_proveedor_con_retorno_de_carro_se_entrecomilla() {
            String csv = csvDe(libroConEntrada(entradaConProveedor("Linea1\rLinea2")));

            assertThat(csv).contains("\"Linea1\rLinea2\",900123456");
        }

        @Test
        @DisplayName("un proveedor sin caracteres especiales va sin comillas")
        void un_proveedor_sin_caracteres_especiales_va_sin_comillas() {
            String csv = csvDe(libroConEntrada(entradaConProveedor("Distribuidora Norte")));

            assertThat(csv).contains("Distribuidora Norte,900123456")
                    .doesNotContain("\"Distribuidora Norte\"");
        }
    }
}
