package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.pregunta;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionHasActiveChildrenException;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Borrar una pregunta con hijos activos dejaría opciones y efectos apuntando a
 * una fila invisible, y el cuestionario cotizando con una rama huérfana.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteConfiguratorQuestionService — baja de una pregunta")
class DeleteConfiguratorQuestionServiceTest {

    @Mock
    private ConfiguratorQuestionRepository repository;
    @Mock
    private ConfiguratorOptionRepository optionRepository;
    @Mock
    private ConfiguratorEffectRepository effectRepository;
    @InjectMocks
    private DeleteConfiguratorQuestionService service;

    @Test
    @DisplayName("borra la pregunta sin hijos activos")
    void borra_la_pregunta_sin_hijos_activos() {
        when(repository.findById(Q1_VENDE)).thenReturn(
                Optional.of(pregunta(Q1_VENDE, "SELLS", AnswerType.SINGLE, null, true)));
        when(optionRepository.existsByQuestionId(Q1_VENDE)).thenReturn(false);
        when(effectRepository.existsByQuestionId(Q1_VENDE)).thenReturn(false);

        service.execute(Q1_VENDE);

        verify(repository).delete(Q1_VENDE);
    }

    @Test
    @DisplayName("una pregunta inexistente no consulta hijos ni borra")
    void una_pregunta_inexistente_no_borra() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(99L))
                .isInstanceOf(ConfiguratorQuestionNotFoundException.class)
                .hasMessageContaining("ConfiguratorQuestion not found: 99");

        verify(repository, never()).delete(any());
        verifyNoInteractions(optionRepository, effectRepository);
    }

    @Test
    @DisplayName("con opciones activas se rechaza y no borra")
    void con_opciones_activas_no_borra() {
        when(repository.findById(Q1_VENDE)).thenReturn(
                Optional.of(pregunta(Q1_VENDE, "SELLS", AnswerType.SINGLE, null, true)));
        when(optionRepository.existsByQuestionId(Q1_VENDE)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(Q1_VENDE))
                .isInstanceOf(ConfiguratorQuestionHasActiveChildrenException.class)
                .hasMessageContaining("Cannot delete ConfiguratorQuestion 1")
                .hasMessageContaining("active option children");

        verify(repository, never()).delete(any());
        verifyNoInteractions(effectRepository);
    }

    @Test
    @DisplayName("con efectos activos colgados de la pregunta se rechaza y no borra")
    void con_efectos_activos_no_borra() {
        when(repository.findById(Q1_VENDE)).thenReturn(
                Optional.of(pregunta(Q1_VENDE, "SELLS", AnswerType.NUMBER, null, true)));
        when(optionRepository.existsByQuestionId(Q1_VENDE)).thenReturn(false);
        when(effectRepository.existsByQuestionId(Q1_VENDE)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(Q1_VENDE))
                .isInstanceOf(ConfiguratorQuestionHasActiveChildrenException.class)
                .hasMessageContaining("active effect children");

        verify(repository, never()).delete(any());
    }
}
