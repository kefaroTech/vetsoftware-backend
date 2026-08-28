package com.vetsoftware.app.subscriptionitemlimit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SubscriptionItemLimit — el techo congelado el día que el cliente firmó")
class SubscriptionItemLimitTest {

    private static final Long ANA = 42L;
    private static final Long LINEA = 8L;
    private static final Long EJE_ANIMAL = 1L;
    private static final LocalDateTime FIRMA = LocalDateTime.of(2026, 9, 1, 8, 0);

    private static SubscriptionItemLimit congeladoEnCien() {
        return SubscriptionItemLimit.freeze(ANA, LINEA, EJE_ANIMAL, MeasureKind.CUMULATIVE,
                LimitMode.LIMITED, 100, null, LimitEnforcement.BLOCK, null, 80, FIRMA);
    }

    @Nested
    @DisplayName("R-LIMIT-21 y R-LIMIT-36 · las mejoras se propagan, los recortes no")
    class PropagacionDeFabrica {

        @Test
        @DisplayName("bajar el modo limitado de 100 a 80 mascotas no le baja el techo a quien"
                + " firmó con 100")
        void bajar_el_modo_limitado_de_100_a_80_mascotas_no_le_baja_el_techo_en_el_proximo_recalculo() {
            SubscriptionItemLimit congelado = congeladoEnCien();

            boolean cambio = congelado.improveFrom(LimitMode.LIMITED, 80);

            assertThat(cambio).isFalse();
            assertThat(congelado.getLimitQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("subir el cupo de fábrica de 100 a 200 sí llega al contrato vivo")
        void subir_el_cupo_de_fabrica_de_100_a_200_llega_al_contrato_vivo() {
            SubscriptionItemLimit congelado = congeladoEnCien();

            boolean cambio = congelado.improveFrom(LimitMode.LIMITED, 200);

            assertThat(cambio).isTrue();
            assertThat(congelado.getLimitQuantity()).isEqualTo(200);
        }

        @Test
        @DisplayName("quitarle el techo al artículo en fábrica es una mejora y llega")
        void quitarle_el_techo_al_articulo_en_fabrica_es_una_mejora() {
            SubscriptionItemLimit congelado = congeladoEnCien();

            boolean cambio = congelado.improveFrom(LimitMode.FULL, null);

            assertThat(cambio).isTrue();
            assertThat(congelado.getMode()).isEqualTo(LimitMode.FULL);
            assertThat(congelado.getLimitQuantity()).isNull();
        }

        @Test
        @DisplayName("poner techo donde el cliente no lo tenía no es una mejora: no toca nada")
        void poner_techo_donde_no_lo_habia_no_es_una_mejora() {
            SubscriptionItemLimit sinTecho = SubscriptionItemLimit.freeze(ANA, LINEA, EJE_ANIMAL,
                    MeasureKind.CUMULATIVE, LimitMode.FULL, null, null, LimitEnforcement.BLOCK,
                    null, 80, FIRMA);

            boolean cambio = sinTecho.improveFrom(LimitMode.LIMITED, 100);

            assertThat(cambio).isFalse();
            assertThat(sinTecho.getMode()).isEqualTo(LimitMode.FULL);
        }

        @Test
        @DisplayName("el mismo cupo de fábrica no cuenta como cambio")
        void el_mismo_cupo_de_fabrica_no_cuenta_como_cambio() {
            assertThat(congeladoEnCien().improveFrom(LimitMode.LIMITED, 100)).isFalse();
        }

        @Test
        @DisplayName("propagar sin decir el modo de fábrica se rechaza")
        void propagar_sin_modo_de_fabrica_se_rechaza() {
            SubscriptionItemLimit congelado = congeladoEnCien();

            assertThatThrownBy(() -> congelado.improveFrom(null, 200))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Validaciones — espejan las cinco restricciones de la tabla")
    class Validaciones {

        @Test
        @DisplayName("LIMITED sin cantidad se rechaza")
        void limited_sin_cantidad_se_rechaza() {
            assertThatThrownBy(() -> SubscriptionItemLimit.freeze(ANA, LINEA, EJE_ANIMAL,
                    MeasureKind.CUMULATIVE, LimitMode.LIMITED, null, null, LimitEnforcement.BLOCK,
                    null, 80, FIRMA)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("el techo de una clínica sin empresa no existe")
        void sin_empresa_se_rechaza() {
            assertThatThrownBy(() -> SubscriptionItemLimit.freeze(null, LINEA, EJE_ANIMAL,
                    MeasureKind.CUMULATIVE, LimitMode.LIMITED, 100, null, LimitEnforcement.BLOCK,
                    null, 80, FIRMA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id");
        }

        @Test
        @DisplayName("OVERAGE sobre un acumulativo se rechaza también en la copia congelada")
        void overage_sobre_acumulativo_se_rechaza() {
            assertThatThrownBy(() -> SubscriptionItemLimit.freeze(ANA, LINEA, EJE_ANIMAL,
                    MeasureKind.CUMULATIVE, LimitMode.LIMITED, 100, null, LimitEnforcement.OVERAGE,
                    new java.math.BigDecimal("500.00"), 80, FIRMA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CUMULATIVE");
        }
    }
}
