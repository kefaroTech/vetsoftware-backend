package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.opcion;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorOptionNotFoundException;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionHasActiveChildrenException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Borrar una opción con una pregunta condicional colgando de ella deja esa
 * pregunta sin condición que la active: el asistente no la muestra nunca más y
 * nadie se entera.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteConfiguratorOptionService — baja de una opcion")
class DeleteConfiguratorOptionServiceTest {

    @Mock
    private ConfiguratorOptionRepository repository;
    @Mock
    private ConfiguratorQuestionRepository questionRepository;
    @Mock
    private ConfiguratorEffectRepository effectRepository;
    @InjectMocks
    private DeleteConfiguratorOptionService service;

    @Test
    @DisplayName("borra la opcion sin efectos ni preguntas colgando")
    void borra_la_opcion_sin_hijos() {
        when(repository.findById(O11_SI_VENDE))
                .thenReturn(Optional.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES")));
        when(effectRepository.existsByOptionId(O11_SI_VENDE)).thenReturn(false);
        when(questionRepository.existsByParentOptionId(O11_SI_VENDE)).thenReturn(false);

        service.execute(O11_SI_VENDE);

        verify(repository).delete(O11_SI_VENDE);
    }

    @Test
    @DisplayName("una opcion inexistente no consulta hijos ni borra")
    void una_opcion_inexistente_no_borra() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(99L))
                .isInstanceOf(ConfiguratorOptionNotFoundException.class)
                .hasMessageContaining("ConfiguratorOption not found: 99");

        verify(repository, never()).delete(any());
        verifyNoInteractions(effectRepository, questionRepository);
    }

    @Test
    @DisplayName("con efectos colgando se rechaza y no borra")
    void con_efectos_colgando_no_borra() {
        when(repository.findById(O11_SI_VENDE))
                .thenReturn(Optional.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES")));
        when(effectRepository.existsByOptionId(O11_SI_VENDE)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(O11_SI_VENDE))
                .isInstanceOf(ConfiguratorQuestionHasActiveChildrenException.class)
                .hasMessageContaining("Cannot delete ConfiguratorOption 11")
                .hasMessageContaining("active effect children");

        verify(repository, never()).delete(any());
        verifyNoInteractions(questionRepository);
    }

    @Test
    @DisplayName("con una pregunta condicional colgando se rechaza y no borra")
    void con_una_pregunta_condicional_colgando_no_borra() {
        when(repository.findById(O11_SI_VENDE))
                .thenReturn(Optional.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES")));
        when(effectRepository.existsByOptionId(O11_SI_VENDE)).thenReturn(false);
        when(questionRepository.existsByParentOptionId(O11_SI_VENDE)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(O11_SI_VENDE))
                .isInstanceOf(ConfiguratorQuestionHasActiveChildrenException.class)
                .hasMessageContaining("active conditional question children");

        verify(repository, never()).delete(any());
    }
}
