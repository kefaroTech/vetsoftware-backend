package com.vetsoftware.app.electronicdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * BE-19 — invariante que la DIAN valida en toda nota: el bloque
 * {@code LegalMonetaryTotal} tiene que cuadrar con la suma de las lineas, y el
 * {@code TaxTotal} con la suma de los impuestos de linea. El adaptador envia
 * los dos bloques por separado ({@code buildLines} y
 * {@code buildMonetaryTotals}), asi que un descuadre de un centavo es un
 * rechazo fiscal — y un rechazo bloquea anular una factura delante del cliente.
 *
 * <p>
 * Se prueba como invariante sobre muchas combinaciones y no con un caso
 * concreto: el descuadre solo aparece con N lineas y un ratio que no divide
 * exacto, que es justo la combinacion que un ejemplo escogido a mano no tiene.
 */
@DisplayName("Notas — invariante Σ lineas == totales (BE-19)")
class ElectronicDocumentNoteTotalsInvariantTest {

    private static final BigDecimal UVT = new BigDecimal("49799");

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static IssuerSnapshot issuer() {
        return new IssuerSnapshot("NIT", "900123456", "7", "Vet SAS", "RESPONSABLE", "vet@x.co",
                List.of("O-13"));
    }

    /** Linea gravada al 19 % con base e IVA explicitos. */
    private static ElectronicDocumentLine ivaLine(int number, String base, String tax) {
        return new ElectronicDocumentLine(null, number, "Gravado " + number, BigDecimal.ONE, "94",
                bd(base), bd(base), TaxCategory.GRAVADO, TaxScheme.IVA, bd("19"), bd(tax),
                bd(base).add(bd(tax)));
    }

    /**
     * Factura validada con {@code n} lineas de importes deliberadamente feos: los
     * centavos son lo que hace aflorar el residuo del prorrateo.
     */
    private static ElectronicDocument validatedInvoice(int n) {
        List<ElectronicDocumentLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 1; i <= n; i++) {
            BigDecimal base = bd("100").multiply(BigDecimal.valueOf(i)).add(bd("0.33"));
            BigDecimal tax = base.multiply(bd("0.19")).setScale(2, java.math.RoundingMode.HALF_UP);
            lines.add(ivaLine(i, base.toPlainString(), tax.toPlainString()));
            total = total.add(base).add(tax);
        }
        ElectronicDocument doc = ElectronicDocument.createPending(9L, 100L,
                ElectronicDocumentType.FE_VENTA, issuer(), CustomerSnapshot.finalConsumer(), lines,
                List.of(new ElectronicDocumentPayment(null, PaymentMeans.EFECTIVO,
                        total.setScale(2, java.math.RoundingMode.HALF_UP))),
                PaymentForm.CONTADO, false, null, null, null, UVT, "req-1", 4L, 7L);
        doc.markValidated("SETP", 990L, "CUFE-ABC123", "cude", "uuid", "<xml/>", "qr", "https://qr",
                "pdf", LocalDateTime.of(2026, 8, 1, 10, 0));
        return doc;
    }

    private static BigDecimal sumOf(ElectronicDocument doc,
            java.util.function.Function<ElectronicDocumentLine, BigDecimal> field) {
        return doc.getLines().stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void assertCuadra(ElectronicDocument note) {
        BigDecimal sumaBases = sumOf(note, ElectronicDocumentLine::getLineExtensionAmount);
        BigDecimal sumaImpuestos = sumOf(note, ElectronicDocumentLine::getTaxAmount);
        BigDecimal sumaTotales = sumOf(note, ElectronicDocumentLine::getTotalAmount);

        assertThat(note.getLineExtensionAmount())
                .as("LineExtensionAmount debe ser la suma de las bases de linea")
                .isEqualByComparingTo(sumaBases);
        assertThat(note.getTaxExclusiveAmount())
                .as("TaxExclusiveAmount debe ser la suma de las bases de linea")
                .isEqualByComparingTo(sumaBases);
        assertThat(note.getTaxInclusiveAmount())
                .as("TaxInclusiveAmount debe ser base + impuestos de las lineas")
                .isEqualByComparingTo(sumaBases.add(sumaImpuestos));
        assertThat(note.getPayableAmount()).as("PayableAmount debe cuadrar con las lineas")
                .isEqualByComparingTo(sumaTotales);
        assertThat(sumaTotales).as("cada linea debe cumplir total == base + impuesto")
                .isEqualByComparingTo(sumaBases.add(sumaImpuestos));
    }

    @Nested
    @DisplayName("nota parcial")
    class NotaParcial {

        static Stream<Arguments> facturasYMontos() {
            List<Arguments> casos = new ArrayList<>();
            // Ratios que no dividen exacto sobre facturas de 1 a 8 lineas.
            for (int lineas = 1; lineas <= 8; lineas++) {
                for (String porcentaje : List.of("0.07", "0.13", "0.3333", "0.5", "0.6667",
                        "0.9999")) {
                    casos.add(Arguments.of(lineas, porcentaje));
                }
            }
            return casos.stream();
        }

        @ParameterizedTest(name = "{0} lineas, {1} del total")
        @MethodSource("facturasYMontos")
        @DisplayName("los totales cuadran con las lineas para cualquier N y cualquier ratio")
        void los_totales_cuadran_con_las_lineas(int lineas, String porcentaje) {
            ElectronicDocument original = validatedInvoice(lineas);
            BigDecimal monto = original.getPayableAmount().multiply(bd(porcentaje)).setScale(2,
                    java.math.RoundingMode.HALF_UP);

            ElectronicDocument note = ElectronicDocument.createCreditNote(original, "2",
                    "Devolucion parcial", 4L, monto);

            assertCuadra(note);
        }

        @ParameterizedTest(name = "{0} lineas, {1} del total")
        @MethodSource("facturasYMontos")
        @DisplayName("cada linea cumple total == base + impuesto")
        void cada_linea_cumple_total_igual_base_mas_impuesto(int lineas, String porcentaje) {
            ElectronicDocument original = validatedInvoice(lineas);
            BigDecimal monto = original.getPayableAmount().multiply(bd(porcentaje)).setScale(2,
                    java.math.RoundingMode.HALF_UP);

            ElectronicDocument note = ElectronicDocument.createCreditNote(original, "2",
                    "Devolucion parcial", 4L, monto);

            assertThat(note.getLines()).allSatisfy(l -> assertThat(l.getTotalAmount())
                    .isEqualByComparingTo(l.getLineExtensionAmount().add(l.getTaxAmount())));
        }

        @ParameterizedTest(name = "{0} lineas, {1} del total")
        @MethodSource("facturasYMontos")
        @DisplayName("la suma de los pagos prorrateados cuadra con el total de la nota")
        void la_suma_de_los_pagos_cuadra_con_el_total(int lineas, String porcentaje) {
            ElectronicDocument original = validatedInvoice(lineas);
            BigDecimal monto = original.getPayableAmount().multiply(bd(porcentaje)).setScale(2,
                    java.math.RoundingMode.HALF_UP);

            ElectronicDocument note = ElectronicDocument.createCreditNote(original, "2",
                    "Devolucion parcial", 4L, monto);

            BigDecimal pagado = note.getPayments().stream()
                    .map(ElectronicDocumentPayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(pagado).isEqualByComparingTo(note.getPayableAmount());
        }

        @ParameterizedTest(name = "{0} lineas, {1} del total")
        @MethodSource("facturasYMontos")
        @DisplayName("el total puede desviarse del monto pedido, pero solo en el residuo del "
                + "prorrateo")
        void el_total_solo_se_desvia_en_el_residuo_del_prorrateo(int lineas, String porcentaje) {
            // Consecuencia asumida del arreglo y conviene tenerla escrita: prorratear
            // linea a linea y sumar no puede dar SIEMPRE el monto exacto que pidio el
            // usuario. Se prefiere un documento que cuadra internamente —lo que la DIAN
            // valida— a uno que borda el monto pedido y sale rechazado. La desviacion
            // esta acotada por el numero de lineas.
            ElectronicDocument original = validatedInvoice(lineas);
            BigDecimal monto = original.getPayableAmount().multiply(bd(porcentaje)).setScale(2,
                    java.math.RoundingMode.HALF_UP);

            ElectronicDocument note = ElectronicDocument.createCreditNote(original, "2",
                    "Devolucion parcial", 4L, monto);

            BigDecimal desviacion = note.getPayableAmount().subtract(monto).abs();
            assertThat(desviacion)
                    .isLessThanOrEqualTo(bd("0.01").multiply(BigDecimal.valueOf(lineas)));
        }

        @ParameterizedTest(name = "nota debito de {1} sobre {0} lineas")
        @MethodSource("facturasYMontos")
        @DisplayName("la nota debito cumple el mismo invariante")
        void la_nota_debito_cumple_el_mismo_invariante(int lineas, String porcentaje) {
            ElectronicDocument original = validatedInvoice(lineas);
            BigDecimal monto = original.getPayableAmount().multiply(bd(porcentaje)).setScale(2,
                    java.math.RoundingMode.HALF_UP);

            ElectronicDocument note = ElectronicDocument.createDebitNote(original, "1", "Mayor", 4L,
                    monto);

            assertCuadra(note);
        }
    }

    @Nested
    @DisplayName("nota total")
    class NotaTotal {

        @ParameterizedTest(name = "{0} lineas")
        @CsvSource({"1", "2", "3", "5", "8"})
        @DisplayName("la nota de anulacion tambien cuadra")
        void la_nota_de_anulacion_tambien_cuadra(int lineas) {
            ElectronicDocument note = ElectronicDocument.createCreditNote(validatedInvoice(lineas),
                    "2", "Anulacion", 4L, null);

            assertCuadra(note);
        }

        @ParameterizedTest(name = "{0} lineas")
        @CsvSource({"1", "2", "3", "5", "8"})
        @DisplayName("el total de la nota sigue siendo IDENTICO al de la factura")
        void el_total_de_la_nota_sigue_siendo_identico_al_de_la_factura(int lineas) {
            // No es cosmetico: CreditNoteReversalApplier decide si una nota anula la
            // venta con note.payable >= original.payable. Si la derivacion desde lineas
            // dejara la nota un centavo por debajo, la anulacion dejaria de reversar
            // inventario y caja EN SILENCIO. Cuadra porque createPending tambien deriva
            // los totales de la factura desde sus lineas; este test es el que avisa si
            // una de las dos derivaciones cambia sin la otra.
            ElectronicDocument original = validatedInvoice(lineas);

            ElectronicDocument note = ElectronicDocument.createCreditNote(original, "2",
                    "Anulacion", 4L, null);

            assertThat(note.getPayableAmount()).isEqualByComparingTo(original.getPayableAmount());
            assertThat(note.getLineExtensionAmount())
                    .isEqualByComparingTo(original.getLineExtensionAmount());
            assertThat(note.getTaxInclusiveAmount())
                    .isEqualByComparingTo(original.getTaxInclusiveAmount());
        }
    }
}
