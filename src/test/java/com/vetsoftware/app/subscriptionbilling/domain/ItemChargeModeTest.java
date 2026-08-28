package com.vetsoftware.app.subscriptionbilling.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * El companion VO de {@code subscription_items.charge_mode}: el unico criterio
 * con el que este slice decide si una linea devenga.
 *
 * <p>
 * <b>Los dos caminos de {@link ItemChargeMode#de(String)} eran los que no
 * tenian red</b>, y son los que deciden el desenlace de un valor que este
 * codigo no entiende. Degradarlo a {@code PAID} seria cobrarle a alguien por un
 * modo desconocido; degradarlo a «no cobra» seria dejar de facturar en
 * silencio. Las dos salidas son peores que parar el cierre, y ninguna de las
 * dos se nota mirando la factura.
 */
@DisplayName("ItemChargeMode — solo PAID devenga, y un valor desconocido para el cierre")
class ItemChargeModeTest {

    @Nested
    @DisplayName("Que devenga")
    class QueDevenga {

        @Test
        @DisplayName("PAID devenga")
        void paid_devenga() {
            assertThat(ItemChargeMode.PAID.generatesCharge()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = ItemChargeMode.class, names = "PAID", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("ninguno de los otros modos devenga")
        void ninguno_de_los_otros_devenga(ItemChargeMode modo) {
            // Matriz y no tres casos sueltos: un quinto modo añadido al enum entra solo
            // y tiene que declarar aqui de que lado cae.
            assertThat(modo.generatesCharge()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(ItemChargeMode.class)
        @DisplayName("exactamente un modo de todo el dominio devenga")
        void exactamente_un_modo_devenga(ItemChargeMode modo) {
            assertThat(modo.generatesCharge()).isEqualTo(modo == ItemChargeMode.PAID);
        }
    }

    @Nested
    @DisplayName("Traduccion del texto crudo de la columna")
    class Traduccion {

        @ParameterizedTest
        @EnumSource(ItemChargeMode.class)
        @DisplayName("cada modo se relee de su propio nombre")
        void cada_modo_se_relee_de_su_nombre(ItemChargeMode modo) {
            assertThat(ItemChargeMode.de(modo.name())).isEqualTo(modo);
        }

        @Test
        @DisplayName("tolera los espacios de una columna CHAR mal recortada")
        void tolera_los_espacios() {
            assertThat(ItemChargeMode.de("  TRIAL  ")).isEqualTo(ItemChargeMode.TRIAL);
        }

        @Test
        @DisplayName("un valor desconocido para el cierre en vez de degradarse a PAID")
        void un_valor_desconocido_para_el_cierre() {
            // El caso caro: un quinto valor escrito por un changeset que este binario no
            // conoce. Si se degradara a PAID, se le cobraria la cuota a quien esta en ese
            // modo; si se degradara a "no cobra", se dejaria de facturar en silencio.
            assertThatThrownBy(() -> ItemChargeMode.de("PROMOCIONAL"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Unknown subscription_items.charge_mode 'PROMOCIONAL'");
        }

        @Test
        @DisplayName("el mensaje del valor desconocido dice por que se para")
        void el_mensaje_dice_por_que_se_para() {
            assertThatThrownBy(() -> ItemChargeMode.de("PROMOCIONAL"))
                    .hasMessageContaining("billing cannot decide whether that line accrues");
        }

        @ParameterizedTest(name = "valor=[{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("una columna vacia tampoco se interpreta: para el cierre igual")
        void una_columna_vacia_para_el_cierre(String valor) {
            assertThatThrownBy(() -> ItemChargeMode.de(valor))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("subscription_items.charge_mode is empty");
        }

        @Test
        @DisplayName("distingue mayusculas: 'paid' no es PAID")
        void distingue_mayusculas() {
            // La columna guarda el name() tal cual. Aceptar 'paid' aqui escondería una
            // fila escrita a mano que el resto del sistema no reconoce.
            assertThatThrownBy(() -> ItemChargeMode.de("paid"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
