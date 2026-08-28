package com.vetsoftware.app.quote.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * D-66 / R-PRICE-04: los tramos son ACUMULATIVOS.
 *
 * <p>
 * El caso que da nombre al defecto: «unidades extra 1 a 8 a 12.000 y de la 9 en
 * adelante a 9.000». Quince usuarios son trece unidades extra —el nucleo trae
 * dos— y se cobran ocho a 12.000 mas cinco a 9.000 = <b>141.000</b>. La
 * aritmetica plana daba 117.000: veinticuatro mil por cliente y mes, unos
 * diecisiete millones al ano a sesenta clinicas, sin error y sin alarma.
 */
@DisplayName("TieredPrice — reparto acumulativo de tramos (D-66)")
class TieredPriceTest {

    private static final LocalDateTime AYER = LocalDateTime.of(2026, 3, 1, 10, 0);
    private static final BigDecimal SIN_DESCUENTO = BigDecimal.ZERO;

    /** «Usuarios 3 a 10» son las unidades extra 1 a 8, con dos incluidas. */
    private static final CatalogPriceRef TRAMO_BAJO = new CatalogPriceRef(
            new BigDecimal("12000.00"), new BigDecimal("19.00"), TaxTreatment.TAXED, 2, 1, 8);
    private static final CatalogPriceRef TRAMO_ALTO = new CatalogPriceRef(new BigDecimal("9000.00"),
            new BigDecimal("19.00"), TaxTreatment.TAXED, 0, 9, null);
    private static final List<CatalogPriceRef> ESCALERA = List.of(TRAMO_BAJO, TRAMO_ALTO);

    private static final CatalogItemRef USUARIO_EXTRA = new CatalogItemRef(77L, "EXTRA_USER",
            "Usuario adicional", QuoteItemType.CAPACITY);

    @Nested
    @DisplayName("La cuenta de D-66")
    class LaCuentaDeD66 {

        @Test
        @DisplayName("quince usuarios con tramo 3 a 10 a 12000 y 11 en adelante a 9000 dan 141000, "
                + "no 135000 ni 117000")
        void quince_usuarios_con_tramo_3_a_10_a_12000_y_11_en_adelante_a_9000_dan_141000_no_135000_ni_117000() {
            TieredPrice repartido = TieredPrice.of(QuoteItemType.CAPACITY, 15, ESCALERA);

            List<QuoteLine> lineas = congelar(repartido, 15);

            assertThat(lineas).hasSize(2);
            assertThat(lineas.get(0).getQuantity()).isEqualTo(8);
            assertThat(lineas.get(0).getUnitAmount()).isEqualByComparingTo("12000.00");
            assertThat(lineas.get(0).grossAmount()).isEqualByComparingTo("96000.00");
            assertThat(lineas.get(1).getQuantity()).isEqualTo(5);
            assertThat(lineas.get(1).getUnitAmount()).isEqualByComparingTo("9000.00");
            assertThat(lineas.get(1).grossAmount()).isEqualByComparingTo("45000.00");

            BigDecimal total = lineas.stream().map(QuoteLine::grossAmount).reduce(BigDecimal.ZERO,
                    BigDecimal::add);
            assertThat(total).isEqualByComparingTo("141000.00");
            assertThat(total).isNotEqualByComparingTo("135000.00");
            assertThat(total).isNotEqualByComparingTo("117000.00");
        }

        @Test
        @DisplayName("la diferencia de 24000 por cliente y mes no ocurre — 17 millones al ano a "
                + "60 clinicas")
        void la_diferencia_de_24000_por_cliente_y_mes_no_ocurre() {
            BigDecimal acumulativo = congelar(TieredPrice.of(QuoteItemType.CAPACITY, 15, ESCALERA),
                    15).stream().map(QuoteLine::grossAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // Lo que devolvia la consulta vieja: trece unidades al precio del tramo alto.
            BigDecimal plano = new BigDecimal("9000.00").multiply(BigDecimal.valueOf(13));

            assertThat(acumulativo.subtract(plano)).isEqualByComparingTo("24000.00");
            assertThat(acumulativo.subtract(plano).multiply(BigDecimal.valueOf(60L * 12L)))
                    .isEqualByComparingTo("17280000.00");
        }

        @Test
        @DisplayName("el tramo se cuenta sobre lo FACTURABLE: lo incluido se resta antes de "
                + "repartir")
        void el_tramo_se_cuenta_sobre_lo_facturable() {
            // Diez usuarios son ocho unidades extra: caben enteras en el tramo bajo.
            TieredPrice repartido = TieredPrice.of(QuoteItemType.CAPACITY, 10, ESCALERA);

            assertThat(repartido.includedQuantity()).isEqualTo(2);
            assertThat(repartido.tiers()).containsExactly(TRAMO_BAJO);
            assertThat(congelar(repartido, 10).get(0).getQuantity()).isEqualTo(8);
        }

        @Test
        @DisplayName("lo incluido sale del tramo que arranca en uno, no del tramo alcanzado")
        void lo_incluido_sale_del_tramo_que_arranca_en_uno() {
            // El tramo alto declara cero incluidas: leerlo de ahi haria que contratar mas
            // unidades cambiase cuantas vienen de regalo.
            assertThat(TieredPrice.of(QuoteItemType.CAPACITY, 15, ESCALERA).includedQuantity())
                    .isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Casos borde del reparto")
    class CasosBorde {

        @Test
        @DisplayName("si lo contratado no supera lo incluido no hay ningun tramo que cobrar")
        void si_lo_contratado_no_supera_lo_incluido_no_hay_tramo() {
            assertThat(TieredPrice.of(QuoteItemType.CAPACITY, 2, ESCALERA).tiers()).isEmpty();
        }

        @Test
        @DisplayName("un articulo sin escalones produce un solo tramo con toda la cantidad")
        void un_articulo_sin_escalones_produce_un_solo_tramo() {
            CatalogPriceRef unico = new CatalogPriceRef(new BigDecimal("69000.00"),
                    new BigDecimal("19.00"), TaxTreatment.TAXED, 0);

            TieredPrice repartido = TieredPrice.of(QuoteItemType.MODULE, 1, List.of(unico));

            assertThat(repartido.tiers()).containsExactly(unico);
        }

        @Test
        @DisplayName("el orden de entrada no decide nada: se ordena por tier_min")
        void el_orden_de_entrada_no_decide_nada() {
            TieredPrice repartido = TieredPrice.of(QuoteItemType.CAPACITY, 15,
                    List.of(TRAMO_ALTO, TRAMO_BAJO));

            assertThat(repartido.tiers()).containsExactly(TRAMO_BAJO, TRAMO_ALTO);
            assertThat(repartido.includedQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("una escalera que no arranca en uno se rechaza en vez de cotizar de menos")
        void una_escalera_que_no_arranca_en_uno_se_rechaza() {
            assertThatThrownBy(
                    () -> TieredPrice.of(QuoteItemType.CAPACITY, 15, List.of(TRAMO_ALTO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must start at 1");
        }

        @Test
        @DisplayName("sin ningun tramo no se cotiza: no hay precio que congelar")
        void sin_ningun_tramo_no_se_cotiza() {
            assertThatThrownBy(() -> TieredPrice.of(QuoteItemType.CAPACITY, 15, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one price tier");
        }
    }

    private static List<QuoteLine> congelar(TieredPrice repartido, int contratadas) {
        List<QuoteLine> lineas = new java.util.ArrayList<>();
        int numero = 1;
        for (CatalogPriceRef tramo : repartido.tiers()) {
            lineas.add(QuoteLine.freeze(numero++, USUARIO_EXTRA, tramo, contratadas,
                    repartido.includedQuantity(), SIN_DESCUENTO, false, AYER));
        }
        return List.copyOf(lineas);
    }
}
