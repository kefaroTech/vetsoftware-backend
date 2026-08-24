package com.vetsoftware.app.configurator.domain;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.CREADA_EL;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q2_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.opcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Una respuesta posible a una pregunta de opción. */
@DisplayName("ConfiguratorOption — una respuesta posible")
class ConfiguratorOptionTest {

    private static final Clock RELOJ = Clock.fixed(CREADA_EL.toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);

    @Nested
    @DisplayName("invariantes de creacion")
    class Validaciones {

        @Test
        @DisplayName("la pregunta a la que pertenece es obligatoria: una opcion suelta no existe")
        void la_pregunta_es_obligatoria() {
            assertThatThrownBy(() -> new ConfiguratorOption(null, null, "YES", "Si", null, 0,
                    CREADA_EL, null, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("questionId is required");
        }

        @ParameterizedTest(name = "code = [{0}]")
        @DisplayName("el code es obligatorio y no puede venir en blanco")
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        void el_code_es_obligatorio(String code) {
            assertThatThrownBy(() -> new ConfiguratorOption(null, Q1_VENDE, code, "Si", null, 0,
                    CREADA_EL, null, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code is required");
        }

        @Test
        @DisplayName("el code no pasa de 50 caracteres")
        void el_code_no_pasa_de_cincuenta() {
            assertThatThrownBy(() -> new ConfiguratorOption(null, Q1_VENDE, "C".repeat(51), "Si",
                    null, 0, CREADA_EL, null, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code must be 50 chars or less");
        }

        @ParameterizedTest(name = "label = [{0}]")
        @DisplayName("la etiqueta es obligatoria: es lo unico que ve el prospecto")
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        void la_etiqueta_es_obligatoria(String label) {
            assertThatThrownBy(() -> new ConfiguratorOption(null, Q1_VENDE, "YES", label, null, 0,
                    CREADA_EL, null, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("label is required");
        }

        @Test
        @DisplayName("la etiqueta no pasa de 255 y la ayuda no pasa de 500")
        void los_maximos_de_etiqueta_y_ayuda() {
            assertThatThrownBy(() -> new ConfiguratorOption(null, Q1_VENDE, "YES", "L".repeat(256),
                    null, 0, CREADA_EL, null, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("label must be 255 chars or less");

            assertThatThrownBy(() -> new ConfiguratorOption(null, Q1_VENDE, "YES", "Si",
                    "H".repeat(501), 0, CREADA_EL, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("helpText must be 500 chars or less");
        }

        @Test
        @DisplayName("el orden no puede ser negativo")
        void el_orden_no_puede_ser_negativo() {
            assertThatThrownBy(() -> new ConfiguratorOption(null, Q1_VENDE, "YES", "Si", null, -1,
                    CREADA_EL, null, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sortOrder cannot be negative");
        }

        @Test
        @DisplayName("el mismo code en dos preguntas distintas es legitimo: la unicidad es por pregunta")
        void el_mismo_code_en_dos_preguntas_distintas_es_legitimo() {
            assertThatCode(() -> {
                opcion(O11_SI_VENDE, Q1_VENDE, "YES");
                opcion(99L, Q2_MOSTRADOR, "YES");
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("create y update")
    class CreacionYEdicion {

        @Test
        @DisplayName("create sella la fecha con el reloj inyectado")
        void create_sella_la_fecha_con_el_reloj_inyectado() {
            ConfiguratorOption nueva = ConfiguratorOption.create(Q1_VENDE, "YES", "Si", "ayuda", 2,
                    RELOJ);

            assertThat(nueva.getCreatedDate()).isEqualTo(CREADA_EL);
            assertThat(nueva.getId()).isNull();
            assertThat(nueva.getVersion()).isNull();
            assertThat(nueva.isEnabled()).isTrue();
            assertThat(nueva.getSortOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("update no mueve la opcion de pregunta ni le cambia el code")
        void update_no_mueve_la_opcion_de_pregunta() {
            ConfiguratorOption opcion = opcion(O11_SI_VENDE, Q1_VENDE, "YES");

            opcion.update("Si, vendo", "ayuda nueva", 5);

            assertThat(opcion.getQuestionId()).isEqualTo(Q1_VENDE);
            assertThat(opcion.getCode()).isEqualTo("YES");
            assertThat(opcion.getLabel()).isEqualTo("Si, vendo");
            assertThat(opcion.getHelpText()).isEqualTo("ayuda nueva");
            assertThat(opcion.getSortOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("update revalida y deja la opcion intacta si la etiqueta nueva no vale")
        void update_revalida_y_deja_la_opcion_intacta() {
            ConfiguratorOption opcion = opcion(O11_SI_VENDE, Q1_VENDE, "YES");

            assertThatThrownBy(() -> opcion.update("  ", null, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("label is required");
            assertThat(opcion.getLabel()).isEqualTo("YES");
        }

        @Test
        @DisplayName("disable y enable mueven la baja logica")
        void disable_y_enable_mueven_la_baja_logica() {
            ConfiguratorOption opcion = opcion(O11_SI_VENDE, Q1_VENDE, "YES");

            opcion.disable();
            assertThat(opcion.isEnabled()).isFalse();

            opcion.enable();
            assertThat(opcion.isEnabled()).isTrue();
        }
    }
}
