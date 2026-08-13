package com.vetsoftware.app.cashregister.infrastructure.web;

import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.OTRO_EMPLEADO_ID;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.movimiento;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionAbierta;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionCerrada;
import static com.vetsoftware.app.cashregister.testsupport.CashSessionMother.sesionConMovimientos;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.cashregister.application.dto.CashArqueoReport;
import com.vetsoftware.app.cashregister.domain.CashMovement;
import com.vetsoftware.app.cashregister.domain.CashMovementType;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.testsupport.CashSessionMother;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

/**
 * El CSV es lo que el contador abre en Excel: si el formato se rompe, el arqueo
 * deja de cuadrar en una hoja que nadie va a depurar leyendo Java.
 */
@DisplayName("CashArqueoCsv — serializacion del arqueo a CSV")
class CashArqueoCsvTest {

    private static String csvDe(CashSession sesion) {
        return new String(CashArqueoCsv.arqueo(CashArqueoReport.from(sesion)),
                StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("formato del archivo")
    class Formato {

        @Test
        @DisplayName("arranca con el BOM UTF-8 para que Excel respete los acentos")
        void arranca_con_el_bom_utf8() {
            byte[] csv = CashArqueoCsv.arqueo(CashArqueoReport.from(sesionCerrada()));

            // Sin BOM, Excel abre el archivo en la codificacion del sistema y "Sesión"
            // sale como "SesiÃ³n". Es el bug clasico de los CSV en Windows.
            assertThat(csv[0]).isEqualTo((byte) 0xEF);
            assertThat(csv[1]).isEqualTo((byte) 0xBB);
            assertThat(csv[2]).isEqualTo((byte) 0xBF);
        }

        @Test
        @DisplayName("separa las filas con CRLF, como manda el RFC 4180")
        void separa_las_filas_con_crlf() {
            assertThat(csvDe(sesionCerrada())).contains("\r\n").startsWith("﻿Arqueo de caja");
        }

        @Test
        @DisplayName("lleva las tres secciones: cabecera, desglose y movimientos")
        void lleva_las_tres_secciones() {
            String csv = csvDe(sesionCerrada());

            assertThat(csv).contains("Arqueo de caja").contains("Base inicial")
                    .contains("Método,Base,Ventas,Abonos,Ingresos,Retiros,Gastos,Reversas,"
                            + "Esperado,Contado,Diferencia")
                    .contains("TOTAL").contains("Fecha,Tipo,Método,Monto,Referencia,Nota");
        }
    }

    @Nested
    @DisplayName("cabecera")
    class Cabecera {

        @Test
        @DisplayName("escribe las fechas en formato legible, no ISO")
        void escribe_las_fechas_en_formato_legible() {
            String csv = csvDe(sesionCerrada());

            assertThat(csv).contains("Apertura,15/01/2026 08:00")
                    .contains("Cierre,15/01/2026 18:00");
        }

        @Test
        @DisplayName("una sesion sin cerrar deja la fecha de cierre en blanco")
        void una_sesion_sin_cerrar_deja_el_cierre_en_blanco() {
            assertThat(csvDe(sesionAbierta())).contains("Cierre,\r\n").contains("Estado,OPEN");
        }
    }

    @Nested
    @DisplayName("desglose por metodo")
    class Desglose {

        @Test
        @DisplayName("traduce los metodos al castellano")
        void traduce_los_metodos_al_castellano() {
            String csv = csvDe(sesionCerrada());

            assertThat(csv).contains("Efectivo,100000,50000").contains("Tarjeta,0,30000");
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({"CASH, Efectivo", "CARD, Tarjeta", "TRANSFER, Transferencia"})
        @DisplayName("cada metodo tiene su etiqueta")
        void cada_metodo_tiene_su_etiqueta(CashPaymentMethod metodo, String etiqueta) {
            assertThat(CashArqueoCsv.methodLabel(metodo)).isEqualTo(etiqueta);
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({"SALE_IN, Venta", "OPEN_ACCOUNT_IN, Abono", "MANUAL_IN, Ingreso",
                "WITHDRAWAL, Retiro", "EXPENSE, Gasto", "VOID_OUT, Reversa"})
        @DisplayName("cada tipo de movimiento tiene su etiqueta")
        void cada_tipo_tiene_su_etiqueta(CashMovementType tipo, String etiqueta) {
            assertThat(CashArqueoCsv.typeLabel(tipo)).isEqualTo(etiqueta);
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("un metodo null no revienta el archivo: sale como celda vacia")
        void un_metodo_null_sale_como_celda_vacia(CashPaymentMethod metodo) {
            assertThat(CashArqueoCsv.methodLabel(metodo)).isEmpty();
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("un tipo null no revienta el archivo: sale como celda vacia")
        void un_tipo_null_sale_como_celda_vacia(CashMovementType tipo) {
            assertThat(CashArqueoCsv.typeLabel(tipo)).isEmpty();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(CashMovementType.class)
        @DisplayName("ningun tipo del enum se queda sin etiqueta")
        void ningun_tipo_se_queda_sin_etiqueta(CashMovementType tipo) {
            // El switch es exhaustivo por construccion, pero esto es lo que falla el dia
            // que alguien añade un tipo y se olvida del CSV.
            assertThat(CashArqueoCsv.typeLabel(tipo)).isNotBlank();
        }

        @Test
        @DisplayName("una sesion abierta deja contado y diferencia vacios, no en cero")
        void una_sesion_abierta_deja_contado_y_diferencia_vacios() {
            String csv = csvDe(sesionConMovimientos());

            // La fila TOTAL termina en dos celdas vacias: nadie ha contado todavia.
            assertThat(csv).contains("TOTAL,,,,,,,,160000,,\r\n");
        }

        @Test
        @DisplayName("la fila TOTAL cierra el desglose con esperado, contado y diferencia")
        void la_fila_total_cierra_el_desglose() {
            assertThat(csvDe(sesionCerrada())).contains("TOTAL,,,,,,,,160000,155000,-5000\r\n");
        }
    }

    @Nested
    @DisplayName("listado de movimientos")
    class Movimientos {

        @Test
        @DisplayName("el monto sale con signo segun el tipo")
        void el_monto_sale_con_signo_segun_el_tipo() {
            String csv = csvDe(sesionConMovimientos());

            // La venta entra en positivo y el retiro en negativo: en la hoja de calculo
            // la columna se suma tal cual y tiene que dar el movimiento neto.
            assertThat(csv).contains("Venta,Efectivo,50000").contains("Retiro,Efectivo,-20000");
        }

        @Test
        @DisplayName("cada movimiento apunta a su documento de origen")
        void la_referencia_apunta_al_documento_de_origen() {
            assertThat(csvDe(sesionConMovimientos())).contains("POS_DOCUMENT #1");
        }

        @Test
        @DisplayName("un movimiento manual no tiene referencia: la celda va vacia")
        void un_movimiento_manual_no_tiene_referencia() {
            assertThat(csvDe(sesionConMovimientos())).contains("Retiro,Efectivo,-20000,,\r\n");
        }
    }

    @Nested
    @DisplayName("escapado RFC 4180")
    class Escapado {

        @Test
        @DisplayName("una nota con coma se entrecomilla para no partir la fila")
        void una_nota_con_coma_se_entrecomilla() {
            CashSession sesion = sesionAbierta();
            sesion.addMovement(conNota("Retiro para banco, sede centro"));

            // Sin comillas, la coma de la nota crearia una columna de mas y desplazaria
            // todo lo que viene detras.
            assertThat(csvDe(sesion)).contains("\"Retiro para banco, sede centro\"");
        }

        @Test
        @DisplayName("una nota con comillas las duplica")
        void una_nota_con_comillas_las_duplica() {
            CashSession sesion = sesionAbierta();
            sesion.addMovement(conNota("Pago del \"domicilio\""));

            assertThat(csvDe(sesion)).contains("\"Pago del \"\"domicilio\"\"\"");
        }

        @Test
        @DisplayName("una nota con salto de linea se entrecomilla")
        void una_nota_con_salto_de_linea_se_entrecomilla() {
            CashSession sesion = sesionAbierta();
            sesion.addMovement(conNota("Primera linea\nSegunda linea"));

            assertThat(csvDe(sesion)).contains("\"Primera linea\nSegunda linea\"");
        }

        @Test
        @DisplayName("una nota sin caracteres especiales va sin comillas")
        void una_nota_sin_caracteres_especiales_va_sin_comillas() {
            CashSession sesion = sesionAbierta();
            sesion.addMovement(conNota("Sencillo"));

            assertThat(csvDe(sesion)).contains(",Sencillo\r\n").doesNotContain("\"Sencillo\"");
        }

        private static CashMovement conNota(String nota) {
            return new CashMovement(null, CashMovementType.WITHDRAWAL, CashPaymentMethod.CASH,
                    new BigDecimal("20000"), CashReferenceType.MANUAL, null, OTRO_EMPLEADO_ID,
                    CashSessionMother.MOVIDA, nota);
        }
    }

    @Nested
    @DisplayName("montos")
    class Montos {

        @Test
        @DisplayName("los ceros de mas se recortan: 100000.00 sale como 100000")
        void los_ceros_de_mas_se_recortan() {
            CashSession sesion = sesionAbierta();
            sesion.addMovement(movimiento(CashMovementType.MANUAL_IN, CashPaymentMethod.CASH,
                    new BigDecimal("15000.00"), CashReferenceType.MANUAL, null));

            // stripTrailingZeros deja el numero limpio en la hoja; sin el, Excel enseña
            // 15000.00 en una moneda que no usa centavos.
            assertThat(csvDe(sesion)).contains("Ingreso,Efectivo,15000,");
        }
    }
}
