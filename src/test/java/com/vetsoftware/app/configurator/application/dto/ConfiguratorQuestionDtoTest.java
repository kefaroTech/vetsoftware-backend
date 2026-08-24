package com.vetsoftware.app.configurator.application.dto;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.CREADA_EL;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q2_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.pregunta;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Proyección administrativa de una pregunta: la que ve la consola. */
@DisplayName("ConfiguratorQuestionDto — proyeccion de una pregunta")
class ConfiguratorQuestionDtoTest {

    @Test
    @DisplayName("copia campo por campo una pregunta de raiz")
    void copia_campo_por_campo_una_pregunta_de_raiz() {
        ConfiguratorQuestionDto dto = ConfiguratorQuestionDto
                .from(pregunta(Q1_VENDE, "SELLS_PRODUCTS", AnswerType.SINGLE, null, true));

        assertThat(dto.id()).isEqualTo(Q1_VENDE);
        assertThat(dto.code()).isEqualTo("SELLS_PRODUCTS");
        assertThat(dto.questionText()).isEqualTo("¿SELLS_PRODUCTS?");
        assertThat(dto.helpText()).isNull();
        assertThat(dto.answerType()).isEqualTo(AnswerType.SINGLE);
        assertThat(dto.parentOptionId()).isNull();
        assertThat(dto.required()).isTrue();
        assertThat(dto.sortOrder()).isZero();
        assertThat(dto.createdDate()).isEqualTo(CREADA_EL);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("conserva la condicion de aparicion de una pregunta condicional")
    void conserva_la_condicion_de_una_pregunta_condicional() {
        ConfiguratorQuestionDto dto = ConfiguratorQuestionDto
                .from(pregunta(Q2_MOSTRADOR, "HAS_COUNTER", AnswerType.MULTI, O11_SI_VENDE, false));

        assertThat(dto.parentOptionId()).isEqualTo(O11_SI_VENDE);
        assertThat(dto.required()).isFalse();
        assertThat(dto.answerType()).isEqualTo(AnswerType.MULTI);
    }

    @Test
    @DisplayName("una pregunta todavia sin persistir sale con id y fecha nulos, no inventados")
    void una_pregunta_sin_persistir_sale_con_id_nulo() {
        ConfiguratorQuestionDto dto = ConfiguratorQuestionDto.from(new ConfiguratorQuestion(null,
                "NUEVA", "texto", "ayuda", AnswerType.BOOLEAN, null, true, 4, null, null, true));

        assertThat(dto.id()).isNull();
        assertThat(dto.createdDate()).isNull();
        assertThat(dto.helpText()).isEqualTo("ayuda");
        assertThat(dto.sortOrder()).isEqualTo(4);
    }
}
