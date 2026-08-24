package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.CREADA_EL;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O21_SI_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q2_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.opcion;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.pregunta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.command.CreateConfiguratorQuestionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorQuestionDto;
import com.vetsoftware.app.configurator.application.dto.LinkStateDto;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConditionalQuestionCycleException;
import com.vetsoftware.app.configurator.domain.ConfiguratorCodeAlreadyExistsException;
import com.vetsoftware.app.configurator.domain.ConfiguratorOptionNotFoundException;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Alta de una pregunta del asistente: código único y rama utilizable. */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateConfiguratorQuestionService — alta de una pregunta")
class CreateConfiguratorQuestionServiceTest {

    @Mock
    private ConfiguratorQuestionRepository repository;
    @Mock
    private ConfiguratorOptionRepository optionRepository;

    private CreateConfiguratorQuestionService service;

    @BeforeEach
    void montarConRelojFijo() {
        service = new CreateConfiguratorQuestionService(repository, optionRepository,
                Clock.fixed(CREADA_EL.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    private static CreateConfiguratorQuestionCommand comando(Long parentOptionId) {
        return new CreateConfiguratorQuestionCommand("SELLS_PRODUCTS", "¿Vende productos?", null,
                AnswerType.SINGLE, parentOptionId, true, 2);
    }

    @Nested
    @DisplayName("alta valida")
    class Creacion {

        @Test
        @DisplayName("guarda la pregunta de raiz con la fecha del reloj inyectado")
        void guarda_la_pregunta_de_raiz() {
            when(repository.findAnyByCode("SELLS_PRODUCTS")).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            ConfiguratorQuestionDto dto = service.execute(comando(null));

            ArgumentCaptor<ConfiguratorQuestion> guardada = ArgumentCaptor
                    .forClass(ConfiguratorQuestion.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getCode()).isEqualTo("SELLS_PRODUCTS");
            assertThat(guardada.getValue().getAnswerType()).isEqualTo(AnswerType.SINGLE);
            assertThat(guardada.getValue().getParentOptionId()).isNull();
            assertThat(guardada.getValue().isRequired()).isTrue();
            assertThat(guardada.getValue().getSortOrder()).isEqualTo(2);
            assertThat(guardada.getValue().getCreatedDate()).isEqualTo(CREADA_EL);
            assertThat(dto.code()).isEqualTo("SELLS_PRODUCTS");

            verifyNoInteractions(optionRepository);
        }

        @Test
        @DisplayName("guarda la pregunta condicional despues de comprobar que su rama esta sana")
        void guarda_la_pregunta_condicional_tras_comprobar_la_rama() {
            when(repository.findAnyByCode("SELLS_PRODUCTS")).thenReturn(Optional.empty());
            when(optionRepository.findById(O21_SI_MOSTRADOR))
                    .thenReturn(Optional.of(opcion(O21_SI_MOSTRADOR, Q2_MOSTRADOR, "YES")));
            when(optionRepository.findAllOrdered())
                    .thenReturn(List.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES"),
                            opcion(O21_SI_MOSTRADOR, Q2_MOSTRADOR, "YES")));
            when(repository.findAllOrdered()).thenReturn(List.of(
                    pregunta(Q1_VENDE, "SELLS", AnswerType.SINGLE, null, true),
                    pregunta(Q2_MOSTRADOR, "COUNTER", AnswerType.SINGLE, O11_SI_VENDE, false)));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            ConfiguratorQuestionDto dto = service.execute(comando(O21_SI_MOSTRADOR));

            assertThat(dto.parentOptionId()).isEqualTo(O21_SI_MOSTRADOR);
        }
    }

    @Nested
    @DisplayName("lo que corta antes de escribir")
    class Validaciones {

        @Test
        @DisplayName("un code repetido se rechaza sin mirar la rama ni guardar")
        void un_code_repetido_se_rechaza() {
            // La guarda ignora el borrado logico: aqui la fila existe Y sigue activa.
            when(repository.findAnyByCode("SELLS_PRODUCTS"))
                    .thenReturn(Optional.of(new LinkStateDto(1L, true)));

            assertThatThrownBy(() -> service.execute(comando(null)))
                    .isInstanceOf(ConfiguratorCodeAlreadyExistsException.class)
                    .hasMessageContaining(
                            "ConfiguratorQuestion code already exists: SELLS_PRODUCTS");

            verify(repository, never()).save(any());
            verifyNoInteractions(optionRepository);
        }

        @Test
        @DisplayName("una opcion padre inexistente se rechaza y no guarda")
        void una_opcion_padre_inexistente_no_guarda() {
            when(repository.findAnyByCode("SELLS_PRODUCTS")).thenReturn(Optional.empty());
            when(optionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(999L)))
                    .isInstanceOf(ConfiguratorOptionNotFoundException.class)
                    .hasMessageContaining("ConfiguratorOption not found: 999");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una rama que ya tiene un ciclo se rechaza: la pregunta nueva naceria inalcanzable")
        void una_rama_con_ciclo_preexistente_se_rechaza() {
            when(repository.findAnyByCode("SELLS_PRODUCTS")).thenReturn(Optional.empty());
            when(optionRepository.findById(O11_SI_VENDE))
                    .thenReturn(Optional.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES")));
            when(optionRepository.findAllOrdered())
                    .thenReturn(List.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES"),
                            opcion(O21_SI_MOSTRADOR, Q2_MOSTRADOR, "YES")));
            // Q1 cuelga de O21 y Q2 de O11: la ascendencia ya esta podrida.
            when(repository.findAllOrdered()).thenReturn(List.of(
                    pregunta(Q1_VENDE, "SELLS", AnswerType.SINGLE, O21_SI_MOSTRADOR, true),
                    pregunta(Q2_MOSTRADOR, "COUNTER", AnswerType.SINGLE, O11_SI_VENDE, false)));

            assertThatThrownBy(() -> service.execute(comando(O11_SI_VENDE)))
                    .isInstanceOf(ConditionalQuestionCycleException.class)
                    .hasMessageContaining("cycle already present above option 11");

            verify(repository, never()).save(any());
        }
    }
}
