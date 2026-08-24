package com.vetsoftware.app.configurator.application.dto;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O21_SI_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q2_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q3_CUANTAS_CAJAS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.opcion;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.pregunta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.configurator.domain.AnswerType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * La proyección pública: lo único que sale al prospecto sin token. No lleva
 * {@code createdDate}, {@code version} ni {@code enabled} a propósito —son
 * datos de administración—, y el tipo de respuesta viaja como texto para que el
 * contrato no se rompa al añadir un valor al enum.
 */
@DisplayName("QuestionnaireQuestionDto — la pregunta tal como la ve el prospecto")
class QuestionnaireQuestionDtoTest {

    @Test
    @DisplayName("copia lo publico de la pregunta y cuelga las opciones que le pasan")
    void copia_lo_publico_y_cuelga_las_opciones() {
        QuestionnaireQuestionDto dto = QuestionnaireQuestionDto.from(
                pregunta(Q2_MOSTRADOR, "HAS_COUNTER", AnswerType.SINGLE, O11_SI_VENDE, false),
                List.of(QuestionnaireOptionDto
                        .from(opcion(O21_SI_MOSTRADOR, Q2_MOSTRADOR, "YES"))));

        assertThat(dto.id()).isEqualTo(Q2_MOSTRADOR);
        assertThat(dto.code()).isEqualTo("HAS_COUNTER");
        assertThat(dto.questionText()).isEqualTo("¿HAS_COUNTER?");
        assertThat(dto.helpText()).isNull();
        assertThat(dto.parentOptionId()).isEqualTo(O11_SI_VENDE);
        assertThat(dto.required()).isFalse();
        assertThat(dto.sortOrder()).isZero();
        assertThat(dto.options()).extracting(QuestionnaireOptionDto::id)
                .containsExactly(O21_SI_MOSTRADOR);
    }

    @ParameterizedTest(name = "answerType = {0}")
    @DisplayName("el tipo de respuesta viaja como el nombre del enum, no como su posicion")
    @EnumSource(AnswerType.class)
    void el_tipo_de_respuesta_viaja_como_nombre(AnswerType tipo) {
        QuestionnaireQuestionDto dto = QuestionnaireQuestionDto
                .from(pregunta(Q3_CUANTAS_CAJAS, "HOW_MANY", tipo, null, true), List.of());

        assertThat(dto.answerType()).isEqualTo(tipo.name());
    }

    @Test
    @DisplayName("null en las opciones se normaliza a lista vacia")
    void null_en_las_opciones_se_normaliza_a_lista_vacia() {
        assertThat(
                new QuestionnaireQuestionDto(1L, "C", "texto", null, "SINGLE", null, true, 0, null)
                        .options())
                .isEmpty();
    }

    @Test
    @DisplayName("las opciones son inmutables aunque se construyan desde una lista mutable")
    void las_opciones_son_inmutables() {
        List<QuestionnaireOptionDto> mutable = new ArrayList<>(List
                .of(QuestionnaireOptionDto.from(opcion(O21_SI_MOSTRADOR, Q2_MOSTRADOR, "YES"))));
        QuestionnaireQuestionDto dto = new QuestionnaireQuestionDto(Q2_MOSTRADOR, "C", "texto",
                null, "SINGLE", null, true, 0, mutable);

        assertThatThrownBy(() -> dto.options()
                .add(QuestionnaireOptionDto.from(opcion(99L, Q2_MOSTRADOR, "NO"))))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
