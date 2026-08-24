package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.opcion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.command.UpdateConfiguratorOptionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorOptionDto;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import com.vetsoftware.app.configurator.domain.ConfiguratorOptionNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Ni el {@code code} ni la pregunta a la que pertenece son editables: mover una
 * opción de pregunta reescribiría el sentido de las respuestas ya guardadas en
 * {@code quote_answers} y de los efectos que cuelgan de ella.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateConfiguratorOptionService — edicion de una opcion")
class UpdateConfiguratorOptionServiceTest {

    @Mock
    private ConfiguratorOptionRepository repository;
    @InjectMocks
    private UpdateConfiguratorOptionService service;

    @Test
    @DisplayName("cambia etiqueta, ayuda y orden sin mover la opcion de pregunta ni cambiarle el code")
    void cambia_lo_editable_y_nada_mas() {
        when(repository.findById(O11_SI_VENDE))
                .thenReturn(Optional.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES")));
        when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        ConfiguratorOptionDto dto = service.execute(
                new UpdateConfiguratorOptionCommand(O11_SI_VENDE, "Si, vendo", "ayuda", 4));

        ArgumentCaptor<ConfiguratorOption> guardada = ArgumentCaptor
                .forClass(ConfiguratorOption.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().getCode()).isEqualTo("YES");
        assertThat(guardada.getValue().getQuestionId()).isEqualTo(Q1_VENDE);
        assertThat(guardada.getValue().getLabel()).isEqualTo("Si, vendo");
        assertThat(guardada.getValue().getHelpText()).isEqualTo("ayuda");
        assertThat(guardada.getValue().getSortOrder()).isEqualTo(4);
        assertThat(dto.label()).isEqualTo("Si, vendo");
    }

    @Test
    @DisplayName("una opcion inexistente no guarda")
    void una_opcion_inexistente_no_guarda() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.execute(new UpdateConfiguratorOptionCommand(99L, "Si", null, 0)))
                .isInstanceOf(ConfiguratorOptionNotFoundException.class)
                .hasMessageContaining("ConfiguratorOption not found: 99");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("una etiqueta en blanco se rechaza y deja la opcion intacta")
    void una_etiqueta_en_blanco_deja_la_opcion_intacta() {
        ConfiguratorOption existente = opcion(O11_SI_VENDE, Q1_VENDE, "YES");
        when(repository.findById(O11_SI_VENDE)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service
                .execute(new UpdateConfiguratorOptionCommand(O11_SI_VENDE, "   ", null, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("label is required");

        verify(repository, never()).save(any());
        assertThat(existente.getLabel()).isEqualTo("YES");
    }
}
