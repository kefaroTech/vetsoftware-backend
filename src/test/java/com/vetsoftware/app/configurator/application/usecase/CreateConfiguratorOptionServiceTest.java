package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.CREADA_EL;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.pregunta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.command.CreateConfiguratorOptionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorOptionDto;
import com.vetsoftware.app.configurator.application.dto.LinkStateDto;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConfiguratorCodeAlreadyExistsException;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionNotFoundException;
import com.vetsoftware.app.configurator.domain.NumberQuestionCannotHaveOptionsException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El {@code code} de una opción es único <em>dentro de su pregunta</em>: dos
 * preguntas pueden tener cada una su {@code YES}, que es lo normal en un
 * cuestionario, y una unicidad global lo impediría.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateConfiguratorOptionService — alta de una opcion")
class CreateConfiguratorOptionServiceTest {

    @Mock
    private ConfiguratorOptionRepository repository;
    @Mock
    private ConfiguratorQuestionRepository questionRepository;

    private CreateConfiguratorOptionService service;

    @BeforeEach
    void montarConRelojFijo() {
        service = new CreateConfiguratorOptionService(repository, questionRepository,
                Clock.fixed(CREADA_EL.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    private static CreateConfiguratorOptionCommand comando() {
        return new CreateConfiguratorOptionCommand(Q1_VENDE, "YES", "Si, vendo", "ayuda", 1);
    }

    @Test
    @DisplayName("guarda la opcion con la fecha del reloj inyectado y colgada de su pregunta")
    void guarda_la_opcion_colgada_de_su_pregunta() {
        when(questionRepository.findById(Q1_VENDE)).thenReturn(
                Optional.of(pregunta(Q1_VENDE, "SELLS", AnswerType.SINGLE, null, true)));
        when(repository.findAnyByQuestionIdAndCode(Q1_VENDE, "YES")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        ConfiguratorOptionDto dto = service.execute(comando());

        ArgumentCaptor<ConfiguratorOption> guardada = ArgumentCaptor
                .forClass(ConfiguratorOption.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().getQuestionId()).isEqualTo(Q1_VENDE);
        assertThat(guardada.getValue().getCode()).isEqualTo("YES");
        assertThat(guardada.getValue().getLabel()).isEqualTo("Si, vendo");
        assertThat(guardada.getValue().getHelpText()).isEqualTo("ayuda");
        assertThat(guardada.getValue().getSortOrder()).isEqualTo(1);
        assertThat(guardada.getValue().getCreatedDate()).isEqualTo(CREADA_EL);
        assertThat(dto.code()).isEqualTo("YES");
    }

    @Test
    @DisplayName("una pregunta inexistente no comprueba el code ni guarda")
    void una_pregunta_inexistente_no_guarda() {
        when(questionRepository.findById(Q1_VENDE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(ConfiguratorQuestionNotFoundException.class)
                .hasMessageContaining("ConfiguratorQuestion not found: 1");

        verify(repository, never()).save(any());
        verify(repository, never()).findAnyByQuestionIdAndCode(any(), any());
    }

    @Test
    @DisplayName("un code repetido dentro de la misma pregunta se rechaza y no guarda")
    void un_code_repetido_en_la_misma_pregunta_no_guarda() {
        when(questionRepository.findById(Q1_VENDE)).thenReturn(
                Optional.of(pregunta(Q1_VENDE, "SELLS", AnswerType.SINGLE, null, true)));
        // La guarda ignora el borrado logico: aqui la fila existe Y sigue activa.
        when(repository.findAnyByQuestionIdAndCode(Q1_VENDE, "YES"))
                .thenReturn(Optional.of(new LinkStateDto(11L, true)));

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(ConfiguratorCodeAlreadyExistsException.class)
                .hasMessageContaining("ConfiguratorOption code already exists: YES");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("colgar la opcion de una pregunta NUMBER se rechaza sin llegar a mirar el code")
    void colgar_la_opcion_de_una_pregunta_number_se_rechaza() {
        // Una pregunta NUMBER se responde escribiendo un numero, asi que una opcion
        // suya no se puede marcar nunca — pero sus efectos si se dispararian si
        // alguien la mandara. Se corta al guardar, no al cotizar, para que el error
        // lo vea quien lo puede arreglar.
        when(questionRepository.findById(Q1_VENDE)).thenReturn(
                Optional.of(pregunta(Q1_VENDE, "HOW_MANY_BOXES", AnswerType.NUMBER, null, false)));

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(NumberQuestionCannotHaveOptionsException.class)
                .hasMessageContaining("Question 1 (HOW_MANY_BOXES)")
                .hasMessageContaining("cannot be a NUMBER question and have options");

        verify(repository, never()).save(any());
        verify(repository, never()).findAnyByQuestionIdAndCode(any(), any());
    }
}
