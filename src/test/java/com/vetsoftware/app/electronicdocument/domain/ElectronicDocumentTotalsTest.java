package com.vetsoftware.app.electronicdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Totalización fiscal del documento: base, total con impuesto, IVA generado,
 * base de retención y neto a pagar. Son las cifras que viajan a la DIAN y las
 * que cuadran la caja, así que se fijan contra mezclas reales de tarifas
 * (GRAVADO 19 %, INC 8 %, EXENTO 0 %, EXCLUIDO sin esquema y líneas libres del
 * POS).
 */
class ElectronicDocumentTotalsTest {

    private static final BigDecimal UVT = new BigDecimal("49799");

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static IssuerSnapshot issuer() {
        return new IssuerSnapshot("NIT", "900123456", "7", "Vet SAS", "RESPONSABLE", "vet@x.co",
                List.of("O-13"));
    }

    /** Línea gravada con IVA: base + IVA a la tarifa indicada. */
    private static ElectronicDocumentLine ivaLine(int number, String base, String rate,
            String tax) {
        return new ElectronicDocumentLine(null, number, "Gravado " + number, BigDecimal.ONE, "94",
                bd(base), bd(base), TaxCategory.GRAVADO, TaxScheme.IVA, bd(rate), bd(tax),
                bd(base).add(bd(tax)));
    }

    /**
     * Línea de impuesto al consumo: entra en la base de retención pero NO en el IVA
     * generado.
     */
    private static ElectronicDocumentLine incLine(int number, String base, String rate,
            String tax) {
        return new ElectronicDocumentLine(null, number, "INC " + number, BigDecimal.ONE, "94",
                bd(base), bd(base), TaxCategory.INC, TaxScheme.INC, bd(rate), bd(tax),
                bd(base).add(bd(tax)));
    }

    /**
     * Exento: conserva esquema IVA con tarifa 0 para que el XML lo distinga de
     * EXCLUIDO.
     */
    private static ElectronicDocumentLine exentoLine(int number, String base) {
        return new ElectronicDocumentLine(null, number, "Exento " + number, BigDecimal.ONE, "94",
                bd(base), bd(base), TaxCategory.EXENTO, TaxScheme.IVA, BigDecimal.ZERO,
                BigDecimal.ZERO, bd(base));
    }

    /**
     * Excluido / ítem libre del POS: sin esquema tributario, fuera de la base de
     * retención.
     */
    private static ElectronicDocumentLine excluidoLine(int number, String base) {
        return new ElectronicDocumentLine(null, number, "Excluido " + number, BigDecimal.ONE, "94",
                bd(base), bd(base), TaxCategory.EXCLUIDO, null, null, BigDecimal.ZERO, bd(base));
    }

    private static ElectronicDocument pending(List<ElectronicDocumentLine> lines,
            List<ElectronicDocumentPayment> payments, boolean withholdingAgent,
            BigDecimal reteFuente, BigDecimal reteIva, BigDecimal reteIca) {
        return ElectronicDocument.createPending(9L, 100L, ElectronicDocumentType.FE_VENTA, issuer(),
                CustomerSnapshot.finalConsumer(), lines, payments, PaymentForm.CONTADO,
                withholdingAgent, reteFuente, reteIva, reteIca, UVT, "req-1", 4L, 7L);
    }

    private static List<ElectronicDocumentPayment> cash(String amount) {
        return List.of(new ElectronicDocumentPayment(null, PaymentMeans.EFECTIVO, bd(amount)));
    }

    @Nested
    class TotalesDesdeLasLineas {

        @Test
        void suma_base_y_total_de_una_sola_linea_gravada() {
            ElectronicDocument doc = pending(List.of(ivaLine(1, "100000", "19", "19000")),
                    cash("119000"), false, null, null, null);

            assertThat(doc.getLineExtensionAmount()).isEqualByComparingTo("100000.00");
            assertThat(doc.getTaxExclusiveAmount()).isEqualByComparingTo("100000.00");
            assertThat(doc.getTaxInclusiveAmount()).isEqualByComparingTo("119000.00");
            assertThat(doc.getPayableAmount()).isEqualByComparingTo("119000.00");
        }

        @Test
        void suma_una_mezcla_de_tarifas() {
            List<ElectronicDocumentLine> lines = List.of(ivaLine(1, "100000", "19", "19000"),
                    incLine(2, "50000", "8", "4000"), exentoLine(3, "30000"),
                    excluidoLine(4, "20000"));

            ElectronicDocument doc = pending(lines, cash("223000"), false, null, null, null);

            assertThat(doc.getLineExtensionAmount()).isEqualByComparingTo("200000.00");
            assertThat(doc.getPayableAmount()).isEqualByComparingTo("223000.00");
        }

        @Test
        void un_documento_sin_lineas_no_puede_construirse() {
            assertThatThrownBy(() -> pending(List.of(), List.of(), false, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one line");
        }
    }

    @Nested
    class CuadreDePagos {

        @Test
        void rechaza_pagos_que_no_igualan_el_total() {
            assertThatThrownBy(() -> pending(List.of(ivaLine(1, "100000", "19", "19000")),
                    cash("100000"), false, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no coincide con el total");
        }

        @Test
        void rechaza_un_pago_de_mas() {
            assertThatThrownBy(() -> pending(List.of(ivaLine(1, "100000", "19", "19000")),
                    cash("200000"), false, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void acepta_pago_mixto_que_suma_el_total() {
            List<ElectronicDocumentPayment> mixto = List.of(
                    new ElectronicDocumentPayment(null, PaymentMeans.EFECTIVO, bd("19000")),
                    new ElectronicDocumentPayment(null, PaymentMeans.TARJETA_DEBITO, bd("100000")));

            ElectronicDocument doc = pending(List.of(ivaLine(1, "100000", "19", "19000")), mixto,
                    false, null, null, null);

            assertThat(doc.getPayments()).hasSize(2);
        }

        @Test
        void sin_pagos_registrados_no_se_valida_el_cuadre() {
            // Emisión al cerrar cuenta: los pagos pueden no viajar en el documento.
            ElectronicDocument doc = pending(List.of(ivaLine(1, "100000", "19", "19000")),
                    List.of(), false, null, null, null);

            assertThat(doc.getPayments()).isEmpty();
        }

        @Test
        void el_medio_de_pago_predominante_es_el_de_mayor_monto() {
            List<ElectronicDocumentPayment> mixto = List.of(
                    new ElectronicDocumentPayment(null, PaymentMeans.EFECTIVO, bd("19000")),
                    new ElectronicDocumentPayment(null, PaymentMeans.TARJETA_DEBITO, bd("100000")));

            ElectronicDocument doc = pending(List.of(ivaLine(1, "100000", "19", "19000")), mixto,
                    false, null, null, null);

            assertThat(doc.primaryPaymentMeansCode())
                    .isEqualTo(PaymentMeans.TARJETA_DEBITO.dianCode());
        }

        @Test
        void sin_pagos_el_medio_predominante_es_efectivo() {
            ElectronicDocument doc = pending(List.of(ivaLine(1, "100000", "19", "19000")),
                    List.of(), false, null, null, null);

            assertThat(doc.primaryPaymentMeansCode()).isEqualTo(PaymentMeans.EFECTIVO.dianCode());
        }
    }

    @Nested
    class IvaGenerado {

        @Test
        void solo_cuenta_las_lineas_con_esquema_iva() {
            List<ElectronicDocumentLine> lines = List.of(ivaLine(1, "100000", "19", "19000"),
                    incLine(2, "50000", "8", "4000"), exentoLine(3, "30000"),
                    excluidoLine(4, "20000"));

            ElectronicDocument doc = pending(lines, cash("223000"), false, null, null, null);

            // El INC (4.000) queda fuera: la reteIVA no puede calcularse sobre IVA+INC.
            assertThat(doc.getIvaTotal()).isEqualByComparingTo("19000.00");
        }

        @Test
        void un_documento_solo_de_inc_tiene_iva_cero() {
            ElectronicDocument doc = pending(List.of(incLine(1, "50000", "8", "4000")),
                    cash("54000"), false, null, null, null);

            assertThat(doc.getIvaTotal()).isEqualByComparingTo("0");
        }
    }

    @Nested
    class BaseDeRetencion {

        @Test
        void excluye_las_lineas_sin_esquema_tributario() {
            List<ElectronicDocumentLine> lines = List.of(ivaLine(1, "100000", "19", "19000"),
                    incLine(2, "50000", "8", "4000"), exentoLine(3, "30000"),
                    excluidoLine(4, "20000"));

            ElectronicDocument doc = pending(lines, cash("223000"), false, null, null, null);

            // 100.000 + 50.000 + 30.000; el excluido (20.000) no forma base gravable.
            assertThat(doc.getWithholdingBase()).isEqualByComparingTo("180000.00");
            assertThat(doc.getWithholdingBase())
                    .isNotEqualByComparingTo(doc.getLineExtensionAmount());
        }

        @Test
        void un_documento_solo_de_excluidos_no_tiene_base_de_retencion() {
            ElectronicDocument doc = pending(List.of(excluidoLine(1, "20000")), cash("20000"),
                    false, null, null, null);

            assertThat(doc.getWithholdingBase()).isEqualByComparingTo("0");
        }
    }

    @Nested
    class NetoAPagar {

        @Test
        void resta_las_tres_retenciones_del_total() {
            List<ElectronicDocumentLine> lines = List.of(ivaLine(1, "1000000", "19", "190000"));

            ElectronicDocument doc = pending(lines, cash("1190000"), true, bd("4"), bd("15"),
                    bd("9.66"));

            assertThat(doc.getReteFuenteAmount()).isEqualByComparingTo("40000.00");
            assertThat(doc.getReteIvaAmount()).isEqualByComparingTo("28500.00");
            assertThat(doc.getReteIcaAmount()).isEqualByComparingTo("9660.00");
            assertThat(doc.getNetPayableAmount()).isEqualByComparingTo("1111840.00");
        }

        @Test
        void sin_agente_retenedor_el_neto_es_el_total() {
            ElectronicDocument doc = pending(List.of(ivaLine(1, "1000000", "19", "190000")),
                    cash("1190000"), false, bd("4"), bd("15"), bd("9.66"));

            assertThat(doc.getNetPayableAmount()).isEqualByComparingTo(doc.getPayableAmount());
        }

        @Test
        void la_reteiva_se_calcula_sobre_el_iva_reportado_por_el_propio_documento() {
            // Coherencia interna: lo retenido debe cuadrar con getIvaTotal(), no con una
            // base distinta.
            List<ElectronicDocumentLine> lines = List.of(ivaLine(1, "1000000", "19", "190000"),
                    incLine(2, "500000", "8", "40000"));

            ElectronicDocument doc = pending(lines, cash("1730000"), true, BigDecimal.ZERO,
                    bd("15"), BigDecimal.ZERO);

            BigDecimal esperado = doc.getIvaTotal().multiply(bd("0.15"));
            assertThat(doc.getReteIvaAmount()).isEqualByComparingTo(esperado);
        }
    }

    @Nested
    class Invariantes {

        @Test
        void una_factura_no_puede_referenciar_otro_documento() {
            assertThatThrownBy(() -> new ElectronicDocument(null, 9L, 100L,
                    ElectronicDocumentType.FE_VENTA, null, null, null,
                    java.time.LocalDate.of(2026, 7, 1), "10:00:00-05:00", null, null, null, null,
                    null, null, null, DianStatus.PENDIENTE, null, issuer(),
                    CustomerSnapshot.finalConsumer(), bd("100"), bd("100"), bd("119"), bd("119"),
                    PaymentForm.CONTADO, List.of(ivaLine(1, "100", "19", "19")), List.of(),
                    java.time.LocalDateTime.now(), true,
                    new DocumentReference("CUFE-X", "SETP", 1L, java.time.LocalDate.of(2026, 6, 1)),
                    null, null, false, null, null, null, null, 4L, 7L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot reference another document");
        }

        @Test
        void las_lineas_del_documento_son_inmutables_desde_fuera() {
            ElectronicDocument doc = pending(List.of(ivaLine(1, "100000", "19", "19000")),
                    cash("119000"), false, null, null, null);

            assertThatThrownBy(() -> doc.getLines().add(ivaLine(2, "1", "19", "0")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void nace_pendiente_sin_numero_ni_sellos_dian() {
            ElectronicDocument doc = pending(List.of(ivaLine(1, "100000", "19", "19000")),
                    cash("119000"), false, null, null, null);

            assertThat(doc.getDianStatus()).isEqualTo(DianStatus.PENDIENTE);
            assertThat(doc.getConsecutive()).isNull();
            assertThat(doc.getPrefix()).isNull();
            assertThat(doc.getCufe()).isNull();
            assertThat(doc.getCude()).isNull();
        }
    }
}
