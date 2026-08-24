package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O12_NO_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O21_SI_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q2_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q3_CUANTAS_CAJAS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.opcion;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.pregunta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.dto.QuestionnaireOptionDto;
import com.vetsoftware.app.configurator.application.dto.QuestionnaireQuestionDto;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.AnswerType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Lo que ve el prospecto antes de existir como usuario. Se arma con dos
 * consultas —preguntas y opciones— y un agrupado en memoria, no con una
 * consulta por pregunta: el cuestionario son decenas de filas y el N+1 aquí se
 * paga en la primera pantalla del embudo de venta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetPublicQuestionnaireService — el cuestionario que ve el prospecto")
class GetPublicQuestionnaireServiceTest {

    @Mock
    private ConfiguratorQuestionRepository questionRepository;
    @Mock
    private ConfiguratorOptionRepository optionRepository;
    @InjectMocks
    private GetPublicQuestionnaireService service;

    @Test
    @DisplayName("cuelga de cada pregunta solo sus propias opciones")
    void cuelga_de_cada_pregunta_solo_sus_opciones() {
        when(questionRepository.findAllOrdered()).thenReturn(List.of(
                pregunta(Q1_VENDE, "SELLS_PRODUCTS", AnswerType.SINGLE, null, true),
                pregunta(Q2_MOSTRADOR, "HAS_COUNTER", AnswerType.SINGLE, O11_SI_VENDE, false)));
        when(optionRepository.findAllOrdered()).thenReturn(
                List.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES"), opcion(O12_NO_VENDE, Q1_VENDE, "NO"),
                        opcion(O21_SI_MOSTRADOR, Q2_MOSTRADOR, "YES")));

        List<QuestionnaireQuestionDto> cuestionario = service.get();

        assertThat(cuestionario).hasSize(2);
        assertThat(cuestionario.get(0).options()).extracting(QuestionnaireOptionDto::id)
                .containsExactly(O11_SI_VENDE, O12_NO_VENDE);
        assertThat(cuestionario.get(1).options()).extracting(QuestionnaireOptionDto::id)
                .containsExactly(O21_SI_MOSTRADOR);
    }

    @Test
    @DisplayName("una pregunta NUMBER sale con la lista de opciones vacia, no con null")
    void una_pregunta_numerica_sale_con_lista_vacia() {
        when(questionRepository.findAllOrdered()).thenReturn(List.of(pregunta(Q3_CUANTAS_CAJAS,
                "HOW_MANY", AnswerType.NUMBER, O21_SI_MOSTRADOR, false)));
        when(optionRepository.findAllOrdered()).thenReturn(List.of());

        List<QuestionnaireQuestionDto> cuestionario = service.get();

        assertThat(cuestionario).singleElement()
                .satisfies(dto -> assertThat(dto.options()).isNotNull().isEmpty());
    }

    @Test
    @DisplayName("respeta el orden en que llegan las preguntas: la BD ya ordeno")
    void respeta_el_orden_en_que_llegan_las_preguntas() {
        when(questionRepository.findAllOrdered()).thenReturn(List.of(
                pregunta(Q2_MOSTRADOR, "HAS_COUNTER", AnswerType.SINGLE, O11_SI_VENDE, false),
                pregunta(Q1_VENDE, "SELLS_PRODUCTS", AnswerType.SINGLE, null, true)));
        when(optionRepository.findAllOrdered()).thenReturn(List.of());

        assertThat(service.get()).extracting(QuestionnaireQuestionDto::code)
                .containsExactly("HAS_COUNTER", "SELLS_PRODUCTS");
    }

    @Test
    @DisplayName("expone el tipo de respuesta y la condicion de aparicion, que es lo que el front necesita")
    void expone_tipo_de_respuesta_y_condicion() {
        when(questionRepository.findAllOrdered()).thenReturn(List.of(
                pregunta(Q3_CUANTAS_CAJAS, "HOW_MANY", AnswerType.NUMBER, O21_SI_MOSTRADOR, true)));
        when(optionRepository.findAllOrdered()).thenReturn(List.of());

        assertThat(service.get()).singleElement().satisfies(dto -> {
            assertThat(dto.answerType()).isEqualTo("NUMBER");
            assertThat(dto.parentOptionId()).isEqualTo(O21_SI_MOSTRADOR);
            assertThat(dto.required()).isTrue();
        });
    }

    @Test
    @DisplayName("un cuestionario todavia vacio devuelve lista vacia, no null")
    void un_cuestionario_vacio_devuelve_lista_vacia() {
        when(questionRepository.findAllOrdered()).thenReturn(List.of());
        when(optionRepository.findAllOrdered()).thenReturn(List.of());

        assertThat(service.get()).isEmpty();
    }
}
