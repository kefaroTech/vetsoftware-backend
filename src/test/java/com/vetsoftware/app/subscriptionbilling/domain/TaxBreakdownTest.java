package com.vetsoftware.app.subscriptionbilling.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TaxBreakdown — el IVA calculado una sola vez, sobre la base agregada")
class TaxBreakdownTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 22, 5, 15, 30);
    private static final ServicePeriod AGOSTO = new ServicePeriod(LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 31));
    private static final Long EMPRESA = 42L;

    private static SubscriptionCharge cargo(ChargeType type, String subtotal,
            TaxTreatment treatment, String rate) {
        return new SubscriptionCharge(null, EMPRESA, 7L, null, type, "linea", AGOSTO,
                BigDecimal.ONE, new BigDecimal(subtotal).abs(), new BigDecimal(subtotal),
                new BigDecimal(rate), treatment, null, ChargeStatus.PENDING, null, null, null,
                AHORA);
    }

    @Nested
    @DisplayName("Calculo")
    class Calculo {

        @Test
        @DisplayName("agrupa por tratamiento y tarifa: una linea por grupo, nunca una por cargo")
        void una_linea_por_grupo() {
            TaxBreakdown breakdown = TaxBreakdown.of(
                    List.of(cargo(ChargeType.RECURRING, "100000.00", TaxTreatment.TAXED, "19.00"),
                            cargo(ChargeType.RECURRING, "50000.00", TaxTreatment.TAXED, "19.00")),
                    DocumentKind.INVOICE, EMPRESA, AHORA);

            assertThat(breakdown.lineas()).hasSize(1);
            assertThat(breakdown.lineas().getFirst().taxableBase())
                    .isEqualByComparingTo("150000.00");
            assertThat(breakdown.lineas().getFirst().taxAmount()).isEqualByComparingTo("28500.00");
        }

        @Test
        @DisplayName("el impuesto sale de la base AGREGADA, no de sumar el de cada linea:"
                + " calcularlo por linea da un peso de diferencia y descuadra la declaracion")
        void el_impuesto_se_calcula_una_sola_vez() {
            // 0.01 * 19% = 0.0019 -> 0.00 redondeado por linea. Tres lineas por
            // separado darian 0.00; la base agregada 0.03 da 0.01. Esa diferencia es
            // exactamente el descuadre que aparece en la declaracion bimestral.
            List<SubscriptionCharge> tres = List.of(
                    cargo(ChargeType.RECURRING, "0.01", TaxTreatment.TAXED, "19.00"),
                    cargo(ChargeType.RECURRING, "0.01", TaxTreatment.TAXED, "19.00"),
                    cargo(ChargeType.RECURRING, "0.01", TaxTreatment.TAXED, "19.00"));

            TaxBreakdown breakdown = TaxBreakdown.of(tres, DocumentKind.INVOICE, EMPRESA, AHORA);

            assertThat(breakdown.taxAmount()).isEqualByComparingTo("0.01");
            BigDecimal sumandoPorLinea = tres.stream()
                    .map(c -> c.getSubtotalAmount().multiply(new BigDecimal("0.19")).setScale(2,
                            java.math.RoundingMode.HALF_UP))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sumandoPorLinea).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("tarifas mixtas en la misma factura: gravado, exento y excluido conviven")
        void tarifas_mixtas() {
            TaxBreakdown breakdown = TaxBreakdown.of(
                    List.of(cargo(ChargeType.RECURRING, "100000.00", TaxTreatment.TAXED, "19.00"),
                            cargo(ChargeType.RECURRING, "20000.00", TaxTreatment.EXEMPT, "0.00"),
                            cargo(ChargeType.RECURRING, "30000.00", TaxTreatment.EXCLUDED, "0.00")),
                    DocumentKind.INVOICE, EMPRESA, AHORA);

            assertThat(breakdown.lineas()).hasSize(3);
            assertThat(breakdown.subtotalAmount()).isEqualByComparingTo("150000.00");
            assertThat(breakdown.taxAmount()).isEqualByComparingTo("19000.00");
            assertThat(breakdown.totalAmount()).isEqualByComparingTo("169000.00");
        }

        @Test
        @DisplayName("excluido y exento NO se colapsan en «tarifa cero»: dos lineas, no una")
        void excluido_y_exento_no_se_colapsan() {
            TaxBreakdown breakdown = TaxBreakdown.of(
                    List.of(cargo(ChargeType.RECURRING, "20000.00", TaxTreatment.EXEMPT, "0.00"),
                            cargo(ChargeType.RECURRING, "30000.00", TaxTreatment.EXCLUDED, "0.00")),
                    DocumentKind.INVOICE, EMPRESA, AHORA);

            assertThat(breakdown.lineas()).hasSize(2).extracting(BillingDocumentTax::taxTreatment)
                    .containsExactlyInAnyOrder(TaxTreatment.EXEMPT, TaxTreatment.EXCLUDED);
            assertThat(breakdown.taxAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("una factura si puede mezclar signos: la cuota con su descuento")
        void la_factura_admite_cuota_con_descuento() {
            TaxBreakdown breakdown = TaxBreakdown.of(
                    List.of(cargo(ChargeType.RECURRING, "100000.00", TaxTreatment.TAXED, "19.00"),
                            cargo(ChargeType.DISCOUNT, "-20000.00", TaxTreatment.TAXED, "19.00")),
                    DocumentKind.INVOICE, EMPRESA, AHORA);

            assertThat(breakdown.subtotalAmount()).isEqualByComparingTo("80000.00");
            assertThat(breakdown.taxAmount()).isEqualByComparingTo("15200.00");
        }
    }

    @Nested
    @DisplayName("Convencion de signos — TRAMPA 1")
    class ConvencionDeSignos {

        @Test
        @DisplayName("el subtotal del documento sale en valor absoluto: el papel siempre es"
                + " positivo y el signo lo da su tipo")
        void el_documento_siempre_es_positivo() {
            TaxBreakdown breakdown = TaxBreakdown.of(
                    List.of(cargo(ChargeType.CREDIT, "-179000.00", TaxTreatment.TAXED, "19.00")),
                    DocumentKind.CREDIT_NOTE, EMPRESA, AHORA);

            assertThat(breakdown.subtotalAmount()).isEqualByComparingTo("179000.00");
            assertThat(breakdown.taxAmount()).isEqualByComparingTo("34010.00");
            assertThat(breakdown.lineas().getFirst().taxableBase())
                    .isEqualByComparingTo("179000.00");
        }

        @Test
        @DisplayName("una nota credito NO puede mezclar cargos de los dos signos")
        void la_nota_credito_no_mezcla_signos() {
            assertThatThrownBy(() -> TaxBreakdown.of(
                    List.of(cargo(ChargeType.CREDIT, "-100000.00", TaxTreatment.TAXED, "19.00"),
                            cargo(ChargeType.RECURRING, "40000.00", TaxTreatment.TAXED, "19.00")),
                    DocumentKind.CREDIT_NOTE, EMPRESA, AHORA))
                    .isInstanceOf(MixedSignChargesException.class)
                    .hasMessageContaining("1 positive and 1 negative");
        }

        @Test
        @DisplayName("una nota credito con solo cargos que suman no es una nota credito")
        void la_nota_credito_agrupa_lo_que_resta() {
            assertThatThrownBy(() -> TaxBreakdown.of(
                    List.of(cargo(ChargeType.RECURRING, "40000.00", TaxTreatment.TAXED, "19.00")),
                    DocumentKind.CREDIT_NOTE, EMPRESA, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("invoice or a debit note");
        }

        @Test
        @DisplayName("un grupo de tarifa que queda del signo contrario al documento se rechaza:"
                + " su valor absoluto seria una base declarable que no existe")
        void ningun_grupo_al_reves_del_documento() {
            assertThatThrownBy(() -> TaxBreakdown.of(
                    List.of(cargo(ChargeType.RECURRING, "100000.00", TaxTreatment.EXCLUDED, "0.00"),
                            cargo(ChargeType.DISCOUNT, "-20000.00", TaxTreatment.TAXED, "19.00")),
                    DocumentKind.INVOICE, EMPRESA, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("opposite sign of the document");
        }

        @Test
        @DisplayName("un documento sin ningun cargo detras es un cobro que nadie puede explicar")
        void sin_cargos_no_hay_documento() {
            assertThatThrownBy(
                    () -> TaxBreakdown.of(List.of(), DocumentKind.INVOICE, EMPRESA, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one charge");
        }
    }

    @Nested
    @DisplayName("Coherencia de la linea")
    class Coherencia {

        @Test
        @DisplayName("una linea no gravada lleva impuesto cero")
        void no_gravado_sin_impuesto() {
            assertThatThrownBy(() -> new BillingDocumentTax(null, EMPRESA, 1L,
                    TaxTreatment.EXCLUDED, BigDecimal.ZERO, new BigDecimal("100.00"),
                    new BigDecimal("19.00"), AHORA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("zero tax amount");
        }

        @Test
        @DisplayName("la base declarada nunca es negativa")
        void base_no_negativa() {
            assertThatThrownBy(() -> new BillingDocumentTax(null, EMPRESA, 1L, TaxTreatment.TAXED,
                    new BigDecimal("19.00"), new BigDecimal("-1.00"), BigDecimal.ZERO, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxableBase");
        }
    }
}
