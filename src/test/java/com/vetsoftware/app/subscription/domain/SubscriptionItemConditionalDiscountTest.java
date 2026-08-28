package com.vetsoftware.app.subscription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D-86 / R-QUOTE-05: el descuento condicionado <b>no muere en el renglon de la
 * cotizacion</b>, viaja congelado a la linea del contrato y bifurca alli la
 * base imponible igual que en la oferta.
 */
@DisplayName("SubscriptionItem — el descuento condicionado llega a la linea (D-86)")
class SubscriptionItemConditionalDiscountTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final Long ARTICULO = 100L;
    private static final LocalDate MAYO_1 = LocalDate.of(2026, 5, 1);
    private static final BigDecimal PRECIO = new BigDecimal("179000.00");
    private static final BigDecimal VEINTE_POR_CIENTO = new BigDecimal("20.00");
    private static final BigDecimal REBAJA = new BigDecimal("35800.00");

    private static SubscriptionItem linea(BigDecimal descuento, boolean condicionado) {
        return SubscriptionItem.open(EMPRESA, CONTRATO, ARTICULO, "PACK_SPA", "Pack Spa",
                SubscriptionItemType.BUNDLE, null, 1, null, 0, TaxTreatment.TAXED, 1, PRECIO,
                descuento.signum() == 0 ? BigDecimal.ZERO : VEINTE_POR_CIENTO, descuento,
                condicionado, new BigDecimal("19.00"), EffectivePeriod.openFrom(MAYO_1),
                ItemOrigin.INITIAL, null);
    }

    @Test
    @DisplayName("un renglon con descuento condicionado produce una linea que lo declara")
    void un_renglon_con_descuento_condicionado_produce_una_linea_que_lo_declara() {
        SubscriptionItem firmada = linea(REBAJA, true);

        assertThat(firmada.isDiscountConditional()).isTrue();
        assertThat(firmada.getDiscountPercent()).isEqualByComparingTo("20.00");
        assertThat(firmada.getDiscountAmount()).isEqualByComparingTo("35800.00");
    }

    @Test
    @DisplayName("con permanencia la base del impuesto es el precio de lista, no el rebajado")
    void con_permanencia_la_base_es_el_precio_de_lista() {
        assertThat(linea(REBAJA, true).taxableBase()).isEqualByComparingTo("179000.00");
    }

    @Test
    @DisplayName("sin condicion el descuento si reduce la base, que es el caso de hoy")
    void sin_condicion_el_descuento_reduce_la_base() {
        assertThat(linea(REBAJA, false).taxableBase()).isEqualByComparingTo("143200.00");
    }

    @Test
    @DisplayName("la cuota recurrente es siempre el neto: la marca cambia el impuesto, no el precio")
    void la_cuota_recurrente_es_siempre_el_neto() {
        assertThat(linea(REBAJA, true).recurringSubtotal()).isEqualByComparingTo("143200.00");
        assertThat(linea(REBAJA, false).recurringSubtotal()).isEqualByComparingTo("143200.00");
    }

    @Test
    @DisplayName("cambiar la cantidad reescala el descuento en pesos en vez de arrastrarlo")
    void cambiar_la_cantidad_reescala_el_descuento() {
        // Arrastrarlo tal cual dejaria un descuento mayor que el bruto en cuanto la
        // cantidad baje, y el constructor lo rechaza.
        SubscriptionItem sucesora = linea(REBAJA, true).withQuantity(2, MAYO_1, 900L);

        assertThat(sucesora.getDiscountAmount()).isEqualByComparingTo("71600.00");
        assertThat(sucesora.isDiscountConditional()).isTrue();
        assertThat(sucesora.taxableBase()).isEqualByComparingTo("358000.00");
    }

    @Test
    @DisplayName("un descuento mayor que el bruto de la linea es imposible de firmar")
    void un_descuento_mayor_que_el_bruto_es_imposible() {
        assertThatThrownBy(() -> linea(new BigDecimal("200000.00"), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("discountAmount cannot exceed");
    }

    @Test
    @DisplayName("el tramo firmado queda escrito en la linea: ya no es columna muerta")
    void el_tramo_firmado_queda_escrito() {
        SubscriptionItem tramoAlto = SubscriptionItem.open(EMPRESA, CONTRATO, ARTICULO,
                "EXTRA_USER", "Usuario adicional", SubscriptionItemType.CAPACITY, "USER", 9, null,
                0, TaxTreatment.TAXED, 5, new BigDecimal("9000.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, false, new BigDecimal("19.00"), EffectivePeriod.openFrom(MAYO_1),
                ItemOrigin.ADDON, 900L);

        assertThat(tramoAlto.getTierMin()).isEqualTo(9);
        assertThat(tramoAlto.getTierMax()).isNull();
        assertThat(tramoAlto.recurringSubtotal()).isEqualByComparingTo("45000.00");
    }
}
