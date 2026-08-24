package com.vetsoftware.app.quote.domain;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.AHORA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.pregunta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QuoteAnswer: por que le vendimos esto")
class QuoteAnswerTest {

    @Test
    @DisplayName("copia el codigo de la pregunta, no solo su id")
    void copia_el_codigo_de_la_pregunta() {
        QuoteAnswer answer = QuoteAnswer.capture(pregunta(), 99L, "SI", AHORA);

        assertThat(answer.getQuestionId()).isEqualTo(11L);
        assertThat(answer.getQuestionCode()).isEqualTo("SELLS_PRODUCTS");
        assertThat(answer.getOptionId()).isEqualTo(99L);
        assertThat(answer.getAnswerValue()).isEqualTo("SI");
    }

    @Test
    @DisplayName("una respuesta numerica no lleva opcion, solo valor")
    void una_respuesta_numerica_solo_lleva_valor() {
        QuoteAnswer answer = QuoteAnswer.capture(pregunta(), null, "3", AHORA);

        assertThat(answer.getOptionId()).isNull();
        assertThat(answer.getAnswerValue()).isEqualTo("3");
    }

    @Test
    @DisplayName("sin opcion y sin valor la respuesta no dice nada y se rechaza")
    void sin_opcion_y_sin_valor_se_rechaza() {
        assertThatThrownBy(() -> QuoteAnswer.capture(pregunta(), null, "  ", AHORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires optionId or answerValue");
    }

    @Test
    @DisplayName("sin pregunta no hay nada que registrar")
    void sin_pregunta_no_hay_respuesta() {
        assertThatThrownBy(() -> QuoteAnswer.capture(null, 1L, "SI", AHORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("configurator question is required");
    }

    @Test
    @DisplayName("el valor literal no puede pasar de 255 caracteres")
    void el_valor_tiene_tope() {
        String largo = "x".repeat(256);

        assertThatThrownBy(() -> QuoteAnswer.capture(pregunta(), null, largo, AHORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("answerValue must be 255 chars or less");
    }
}
