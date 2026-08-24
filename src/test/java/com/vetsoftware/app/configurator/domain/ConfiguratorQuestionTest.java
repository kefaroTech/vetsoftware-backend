package com.vetsoftware.app.configurator.domain;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.CREADA_EL;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Una pregunta del asistente de venta: texto, orden y condición son datos. */
@DisplayName("ConfiguratorQuestion — la pregunta del asistente de venta")
class ConfiguratorQuestionTest {

    private static final Clock RELOJ = Clock.fixed(CREADA_EL.toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);

    private static ConfiguratorQuestion valida() {
        return new ConfiguratorQuestion(Q1_VENDE, "SELLS_PRODUCTS", "¿Vende productos?", null,
                AnswerType.SINGLE, null, true, 0, CREADA_EL, 0L, true);
    }

    @Nested
    @DisplayName("invariantes de creacion")
    class Validaciones {

        @ParameterizedTest(name = "code = [{0}]")
        @DisplayName("el code es obligatorio y no puede venir en blanco")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void el_code_es_obligatorio(String code) {
            assertThatThrownBy(() -> new ConfiguratorQuestion(null, code, "texto", null,
                    AnswerType.SINGLE, null, true, 0, CREADA_EL, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code is required");
        }

        @Test
        @DisplayName("el code no pasa de 50 caracteres, que es lo que aguanta la columna")
        void el_code_no_pasa_de_cincuenta() {
            assertThatThrownBy(() -> new ConfiguratorQuestion(null, "C".repeat(51), "texto", null,
                    AnswerType.SINGLE, null, true, 0, CREADA_EL, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code must be 50 chars or less");
        }

        @Test
        @DisplayName("un code de exactamente 50 caracteres si entra")
        void un_code_de_cincuenta_entra() {
            assertThatCode(() -> new ConfiguratorQuestion(null, "C".repeat(50), "texto", null,
                    AnswerType.SINGLE, null, true, 0, CREADA_EL, null, true))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "questionText = [{0}]")
        @DisplayName("el texto de la pregunta es obligatorio")
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        void el_texto_es_obligatorio(String texto) {
            assertThatThrownBy(() -> new ConfiguratorQuestion(null, "CODE", texto, null,
                    AnswerType.SINGLE, null, true, 0, CREADA_EL, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("questionText is required");
        }

        @Test
        @DisplayName("el texto de la pregunta no pasa de 255")
        void el_texto_no_pasa_de_doscientos_cincuenta_y_cinco() {
            assertThatThrownBy(() -> new ConfiguratorQuestion(null, "CODE", "T".repeat(256), null,
                    AnswerType.SINGLE, null, true, 0, CREADA_EL, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("questionText must be 255 chars or less");
        }

        @Test
        @DisplayName("la ayuda es opcional pero no pasa de 500")
        void la_ayuda_es_opcional_pero_no_pasa_de_quinientos() {
            assertThatCode(() -> new ConfiguratorQuestion(null, "CODE", "texto", null,
                    AnswerType.SINGLE, null, true, 0, CREADA_EL, null, true))
                    .doesNotThrowAnyException();

            assertThatThrownBy(() -> new ConfiguratorQuestion(null, "CODE", "texto",
                    "H".repeat(501), AnswerType.SINGLE, null, true, 0, CREADA_EL, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("helpText must be 500 chars or less");
        }

        @Test
        @DisplayName("el tipo de respuesta es obligatorio")
        void el_tipo_de_respuesta_es_obligatorio() {
            assertThatThrownBy(() -> new ConfiguratorQuestion(null, "CODE", "texto", null, null,
                    null, true, 0, CREADA_EL, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("answerType is required");
        }

        @Test
        @DisplayName("el orden no puede ser negativo")
        void el_orden_no_puede_ser_negativo() {
            assertThatThrownBy(() -> new ConfiguratorQuestion(null, "CODE", "texto", null,
                    AnswerType.SINGLE, null, true, -1, CREADA_EL, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sortOrder cannot be negative");
        }

        @ParameterizedTest(name = "answerType = {0}")
        @DisplayName("los cuatro tipos de respuesta se aceptan")
        @EnumSource(AnswerType.class)
        void los_cuatro_tipos_se_aceptan(AnswerType tipo) {
            assertThat(new ConfiguratorQuestion(null, "CODE", "texto", null, tipo, null, true, 0,
                    CREADA_EL, null, true).getAnswerType()).isEqualTo(tipo);
        }
    }

    @Nested
    @DisplayName("condicionalidad")
    class Condicionalidad {

        @Test
        @DisplayName("sin opcion padre la pregunta es de raiz")
        void sin_opcion_padre_es_de_raiz() {
            assertThat(valida().isConditional()).isFalse();
        }

        @Test
        @DisplayName("con opcion padre la pregunta es condicional")
        void con_opcion_padre_es_condicional() {
            ConfiguratorQuestion condicional = new ConfiguratorQuestion(2L, "HAS_COUNTER", "texto",
                    null, AnswerType.SINGLE, O11_SI_VENDE, false, 1, CREADA_EL, 0L, true);

            assertThat(condicional.isConditional()).isTrue();
            assertThat(condicional.getParentOptionId()).isEqualTo(O11_SI_VENDE);
        }

        @Test
        @DisplayName("update puede sacar una pregunta de su rama y devolverla a la raiz")
        void update_puede_devolver_la_pregunta_a_la_raiz() {
            ConfiguratorQuestion condicional = new ConfiguratorQuestion(2L, "HAS_COUNTER", "texto",
                    null, AnswerType.SINGLE, O11_SI_VENDE, false, 1, CREADA_EL, 0L, true);

            condicional.update("texto", null, AnswerType.SINGLE, null, false, 1);

            assertThat(condicional.isConditional()).isFalse();
        }
    }

    @Nested
    @DisplayName("create y update")
    class CreacionYEdicion {

        @Test
        @DisplayName("create sella la fecha con el reloj inyectado")
        void create_sella_la_fecha_con_el_reloj_inyectado() {
            ConfiguratorQuestion nueva = ConfiguratorQuestion.create("CODE", "texto", null,
                    AnswerType.NUMBER, null, true, 3, RELOJ);

            assertThat(nueva.getCreatedDate()).isEqualTo(CREADA_EL);
            assertThat(nueva.getId()).isNull();
            assertThat(nueva.getVersion()).isNull();
            assertThat(nueva.isEnabled()).isTrue();
            assertThat(nueva.getSortOrder()).isEqualTo(3);
        }

        @Test
        @DisplayName("update no toca el code: lo copian las cotizaciones ya emitidas")
        void update_no_toca_el_code() {
            ConfiguratorQuestion pregunta = valida();

            pregunta.update("otro texto", "ayuda", AnswerType.NUMBER, null, false, 9);

            assertThat(pregunta.getCode()).isEqualTo("SELLS_PRODUCTS");
            assertThat(pregunta.getQuestionText()).isEqualTo("otro texto");
            assertThat(pregunta.getHelpText()).isEqualTo("ayuda");
            assertThat(pregunta.getAnswerType()).isEqualTo(AnswerType.NUMBER);
            assertThat(pregunta.isRequired()).isFalse();
            assertThat(pregunta.getSortOrder()).isEqualTo(9);
        }

        @Test
        @DisplayName("update revalida y deja la pregunta intacta si el texto nuevo no vale")
        void update_revalida_y_deja_la_pregunta_intacta() {
            ConfiguratorQuestion pregunta = valida();

            assertThatThrownBy(() -> pregunta.update("  ", null, AnswerType.SINGLE, null, true, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("questionText is required");
            assertThat(pregunta.getQuestionText()).isEqualTo("¿Vende productos?");
        }

        @Test
        @DisplayName("disable y enable mueven la baja logica")
        void disable_y_enable_mueven_la_baja_logica() {
            ConfiguratorQuestion pregunta = valida();

            pregunta.disable();
            assertThat(pregunta.isEnabled()).isFalse();

            pregunta.enable();
            assertThat(pregunta.isEnabled()).isTrue();
        }
    }
}
