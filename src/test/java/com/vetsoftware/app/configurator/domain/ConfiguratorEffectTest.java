package com.vetsoftware.app.configurator.domain;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.CREADA_EL;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_POS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q3_CUANTAS_CAJAS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.efectoPorOpcion;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.efectoPorPregunta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Las invariantes que la base no puede vigilar sola y que, si se saltan,
 * duplican un artículo en una cotización firmada.
 */
@DisplayName("ConfiguratorEffect — la traduccion de una respuesta en un articulo")
class ConfiguratorEffectTest {

    private static final Clock RELOJ = Clock.fixed(CREADA_EL.toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);

    @Nested
    @DisplayName("el disparador es exactamente uno")
    class Disparador {

        @Test
        @DisplayName("con los dos disparadores rellenos se rechaza: el articulo entraria dos veces")
        void con_los_dos_disparadores_se_rechaza() {
            assertThatThrownBy(() -> new ConfiguratorEffect(1L, O11_SI_VENDE, Q3_CUANTAS_CAJAS,
                    ITEM_POS, EffectType.ADD, null, CREADA_EL, 0L, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exactly one trigger is required");
        }

        @Test
        @DisplayName("sin ningun disparador se rechaza: nada lo activaria nunca")
        void sin_ningun_disparador_se_rechaza() {
            assertThatThrownBy(() -> new ConfiguratorEffect(1L, null, null, ITEM_POS,
                    EffectType.ADD, null, CREADA_EL, 0L, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exactly one trigger is required");
        }

        @Test
        @DisplayName("isTriggeredByQuestion distingue el efecto numerico del de opcion")
        void is_triggered_by_question_distingue_los_dos() {
            assertThat(efectoPorPregunta(1L, Q3_CUANTAS_CAJAS, ITEM_POS,
                    EffectType.QUANTITY_FROM_ANSWER, null).isTriggeredByQuestion()).isTrue();
            assertThat(efectoPorOpcion(2L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null)
                    .isTriggeredByQuestion()).isFalse();
        }
    }

    @Nested
    @DisplayName("la cantidad solo la lleva SET_QUANTITY")
    class Cantidad {

        @ParameterizedTest(name = "SET_QUANTITY con cantidad {0}")
        @DisplayName("SET_QUANTITY exige una cantidad mayor que cero")
        @ValueSource(ints = {0, -1, -50})
        void set_quantity_exige_cantidad_positiva(int cantidad) {
            assertThatThrownBy(() -> efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS,
                    EffectType.SET_QUANTITY, cantidad)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SET_QUANTITY requires a quantity greater than 0");
        }

        @Test
        @DisplayName("SET_QUANTITY sin cantidad se rechaza")
        void set_quantity_sin_cantidad_se_rechaza() {
            assertThatThrownBy(() -> efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS,
                    EffectType.SET_QUANTITY, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SET_QUANTITY requires a quantity");
        }

        @ParameterizedTest(name = "{0} con cantidad")
        @DisplayName("cualquier otro efecto con cantidad se rechaza: seria un dato muerto")
        @EnumSource(value = EffectType.class, names = "SET_QUANTITY", mode = EnumSource.Mode.EXCLUDE)
        void otro_efecto_con_cantidad_se_rechaza(EffectType tipo) {
            assertThatThrownBy(() -> efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, tipo, 3))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity is only allowed for SET_QUANTITY");
        }

        @ParameterizedTest(name = "{0} sin cantidad")
        @DisplayName("cualquier otro efecto sin cantidad es valido")
        @EnumSource(value = EffectType.class, names = "SET_QUANTITY", mode = EnumSource.Mode.EXCLUDE)
        void otro_efecto_sin_cantidad_es_valido(EffectType tipo) {
            assertThat(efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, tipo, null).getQuantity())
                    .isNull();
        }
    }

    @Nested
    @DisplayName("lo minimo obligatorio")
    class Obligatorios {

        @Test
        @DisplayName("sin articulo de catalogo se rechaza")
        void sin_articulo_se_rechaza() {
            assertThatThrownBy(() -> efectoPorOpcion(1L, O11_SI_VENDE, null, EffectType.ADD, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("catalogItemId is required");
        }

        @Test
        @DisplayName("sin tipo de efecto se rechaza")
        void sin_tipo_de_efecto_se_rechaza() {
            assertThatThrownBy(() -> efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("effect is required");
        }
    }

    @Nested
    @DisplayName("create y update")
    class CreacionYEdicion {

        @Test
        @DisplayName("create sella la fecha con el reloj inyectado y nace habilitado")
        void create_sella_la_fecha_con_el_reloj_inyectado() {
            ConfiguratorEffect efecto = ConfiguratorEffect.create(O11_SI_VENDE, null, ITEM_POS,
                    EffectType.ADD, null, RELOJ);

            assertThat(efecto.getCreatedDate()).isEqualTo(CREADA_EL);
            assertThat(efecto.getId()).isNull();
            assertThat(efecto.getVersion()).isNull();
            assertThat(efecto.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("update cambia articulo, tipo y cantidad sin tocar el disparador")
        void update_no_toca_el_disparador() {
            ConfiguratorEffect efecto = efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD,
                    null);

            efecto.update(999L, EffectType.SET_QUANTITY, 7);

            assertThat(efecto.getCatalogItemId()).isEqualTo(999L);
            assertThat(efecto.getEffect()).isEqualTo(EffectType.SET_QUANTITY);
            assertThat(efecto.getQuantity()).isEqualTo(7);
            assertThat(efecto.getOptionId()).isEqualTo(O11_SI_VENDE);
            assertThat(efecto.getQuestionId()).isNull();
        }

        @Test
        @DisplayName("update revalida: no se puede dejar el efecto sin articulo")
        void update_revalida() {
            ConfiguratorEffect efecto = efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD,
                    null);

            assertThatThrownBy(() -> efecto.update(null, EffectType.ADD, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("catalogItemId is required");
            assertThat(efecto.getCatalogItemId()).isEqualTo(ITEM_POS);
        }

        @Test
        @DisplayName("disable y enable mueven la baja logica")
        void disable_y_enable_mueven_la_baja_logica() {
            ConfiguratorEffect efecto = efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD,
                    null);

            efecto.disable();
            assertThat(efecto.isEnabled()).isFalse();

            efecto.enable();
            assertThat(efecto.isEnabled()).isTrue();
        }
    }
}
