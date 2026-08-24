package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.CREADA_EL;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q2_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.pregunta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.dto.ConfiguratorQuestionDto;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Consulta por id de una pregunta del cuestionario. */
@ExtendWith(MockitoExtension.class)
@DisplayName("FindConfiguratorQuestionService — consulta de una pregunta por id")
class FindConfiguratorQuestionServiceTest {

    @Mock
    private ConfiguratorQuestionRepository repository;

    // La pregunta sale con sus opciones anidadas (#448): sin este doble,
    // @InjectMocks deja el puerto a null y el caso de uso revienta con NPE.
    @Mock
    private ConfiguratorOptionRepository optionRepository;

    @InjectMocks
    private FindConfiguratorQuestionService service;

    @Test
    @DisplayName("devuelve la pregunta traducida a DTO, campo por campo")
    void devuelve_la_pregunta_traducida_a_dto() {
        when(repository.findById(Q2_MOSTRADOR)).thenReturn(Optional
                .of(pregunta(Q2_MOSTRADOR, "HAS_COUNTER", AnswerType.SINGLE, O11_SI_VENDE, false)));

        ConfiguratorQuestionDto dto = service.findById(Q2_MOSTRADOR);

        assertThat(dto.id()).isEqualTo(Q2_MOSTRADOR);
        assertThat(dto.code()).isEqualTo("HAS_COUNTER");
        assertThat(dto.answerType()).isEqualTo(AnswerType.SINGLE);
        assertThat(dto.parentOptionId()).isEqualTo(O11_SI_VENDE);
        assertThat(dto.required()).isFalse();
        assertThat(dto.createdDate()).isEqualTo(CREADA_EL);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("una pregunta inexistente da un 404 con el id dentro del mensaje")
    void una_pregunta_inexistente_da_404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ConfiguratorQuestionNotFoundException.class)
                .hasMessageContaining("ConfiguratorQuestion not found: 99");
    }
}
