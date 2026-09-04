package com.vetsoftware.app.quote.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D-86 / R-TAX-04 / R-QUOTE-05: el descuento CONDICIONADO no reduce la base del
 * IVA.
 *
 * <p>
 * La norma solo excluye de la base los descuentos «siempre y cuando no esten
 * sujetos a ninguna condicion». Un veinte por ciento a cambio de quedarse doce
 * meses esta condicionado por definicion, asi que el impuesto se liquida sobre
 * el precio de lista.
 */
@DisplayName("QuoteLine — descuento condicionado y base imponible (D-86)")
class QuoteLineConditionalDiscountTest {

    private static final LocalDateTime AYER = LocalDateTime.of(2026, 3, 1, 10, 0);
    private static final BigDecimal VEINTE_POR_CIENTO = new BigDecimal("20.00");

    private static final CatalogItemRef PACK_SPA = new CatalogItemRef(42L, "PACK_SPA",
            "Estetica y guarderia", QuoteItemType.BUNDLE);
    private static final CatalogPriceRef A_179000 = new CatalogPriceRef(new BigDecimal("179000.00"),
            new BigDecimal("19.00"), TaxTreatment.TAXED, 0);

    @Test
    @DisplayName("un 20 por ciento por permanencia sobre 179000 liquida iva sobre 179000, no sobre "
            + "143200")
    void un_20_por_ciento_por_permanencia_sobre_179000_liquida_iva_sobre_179000_no_sobre_143200() {
        QuoteLine conPermanencia = QuoteLine.freeze(1, PACK_SPA, A_179000, 1, 0, VEINTE_POR_CIENTO,
                true, AYER);

        assertThat(conPermanencia.grossAmount()).isEqualByComparingTo("179000.00");
        assertThat(conPermanencia.getDiscountAmount()).isEqualByComparingTo("35800.00");
        assertThat(conPermanencia.taxableBase()).isEqualByComparingTo("179000.00");
        assertThat(conPermanencia.getTaxAmount()).isEqualByComparingTo("34010.00");
        // Lo que el cliente PAGA sigue siendo el neto mas el impuesto.
        assertThat(conPermanencia.getLineTotal()).isEqualByComparingTo("177210.00");
        assertThat(conPermanencia.isDiscountConditional()).isTrue();
    }

    @Test
    @DisplayName("veinte clientes rebajados en un semestre no dejan 816240 de iva sin liquidar")
    void veinte_clientes_rebajados_en_un_semestre_no_dejan_816240_de_iva_sin_liquidar() {
        QuoteLine condicionado = QuoteLine.freeze(1, PACK_SPA, A_179000, 1, 0, VEINTE_POR_CIENTO,
                true, AYER);
        QuoteLine incondicionado = QuoteLine.freeze(1, PACK_SPA, A_179000, 1, 0, VEINTE_POR_CIENTO,
                false, AYER);

        BigDecimal porClienteYMes = condicionado.getTaxAmount()
                .subtract(incondicionado.getTaxAmount());

        assertThat(porClienteYMes).isEqualByComparingTo("6802.00");
        assertThat(porClienteYMes.multiply(BigDecimal.valueOf(20L * 6L)))
                .isEqualByComparingTo("816240.00");
    }

    @Test
    @DisplayName("un descuento incondicionado sigue reduciendo la base, que es el caso de todo el "
            + "catalogo de hoy")
    void un_descuento_incondicionado_sigue_reduciendo_la_base() {
        QuoteLine rebaja = QuoteLine.freeze(1, PACK_SPA, A_179000, 1, 0, VEINTE_POR_CIENTO, false,
                AYER);

        assertThat(rebaja.taxableBase()).isEqualByComparingTo("143200.00");
        assertThat(rebaja.getTaxAmount()).isEqualByComparingTo("27208.00");
        assertThat(rebaja.getLineTotal()).isEqualByComparingTo("170408.00");
        assertThat(rebaja.isDiscountConditional()).isFalse();
    }

    @Test
    @DisplayName("sin descuento la marca no cambia nada: base, impuesto y total son los mismos")
    void sin_descuento_la_marca_no_cambia_nada() {
        QuoteLine marcada = QuoteLine.freeze(1, PACK_SPA, A_179000, 1, 0, BigDecimal.ZERO, true,
                AYER);
        QuoteLine sinMarcar = QuoteLine.freeze(1, PACK_SPA, A_179000, 1, 0, BigDecimal.ZERO, false,
                AYER);

        assertThat(marcada.taxableBase()).isEqualByComparingTo(sinMarcar.taxableBase());
        assertThat(marcada.getTaxAmount()).isEqualByComparingTo(sinMarcar.getTaxAmount());
        assertThat(marcada.getLineTotal()).isEqualByComparingTo(sinMarcar.getLineTotal());
    }

    @Test
    @DisplayName("la aritmetica se vuelve a comprobar AL LEER: una fila con el impuesto del "
            + "importe rebajado y la marca puesta se rechaza")
    void la_aritmetica_se_vuelve_a_comprobar_al_leer() {
        // Exactamente la fila que producia el defecto: marca condicionada e impuesto
        // calculado sobre la base rebajada. Reconstruirla desde la base tiene que
        // fallar.
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> new QuoteLine(9L, 1, 42L, "PACK_SPA",
                        "Estetica y guarderia", QuoteItemType.BUNDLE, 1, null, 1, 0, 1,
                        new BigDecimal("179000.00"), VEINTE_POR_CIENTO, new BigDecimal("35800.00"),
                        true, new BigDecimal("19.00"), TaxTreatment.TAXED,
                        new BigDecimal("27208.00"), new BigDecimal("170408.00"), AYER, true))
                .isInstanceOf(QuoteLineArithmeticException.class).hasMessageContaining("taxAmount");
    }
}
