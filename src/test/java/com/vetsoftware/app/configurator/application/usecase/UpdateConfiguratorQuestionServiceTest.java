package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O21_SI_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q2_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q3_CUANTAS_CAJAS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.opcion;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.pregunta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.command.UpdateConfiguratorQuestionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorQuestionDto;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConditionalQuestionCycleException;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionNotFoundException;
import com.vetsoftware.app.configurator.domain.NumberQuestionCannotHaveOptionsException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La edición es el camino por el que un ciclo entra de verdad: el alta no puede
 * cerrarlo porque nadie apunta todavía a la pregunta nueva.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateConfiguratorQuestionService — edicion de una pregunta")
class UpdateConfiguratorQuestionServiceTest {

    @Mock
    private ConfiguratorQuestionRepository repository;
    @Mock
    private ConfiguratorOptionRepository optionRepository;
    @Mock
    private ConfiguratorEffectRepository effectRepository;
    @InjectMocks
    private UpdateConfiguratorQuestionService service;

    private static UpdateConfiguratorQuestionCommand comando(Long id, Long parentOptionId) {
        return new UpdateConfiguratorQuestionCommand(id, "¿Vende productos al mostrador?", "ayuda",
                AnswerType.SINGLE, parentOptionId, false, 7);
    }

    @Nested
    @DisplayName("edicion valida")
    class Edicion {

        @Test
        @DisplayName("cambia texto, ayuda, obligatoriedad y orden sin tocar el code")
        void cambia_lo_editable_sin_tocar_el_code() {
            when(repository.findById(Q1_VENDE)).thenReturn(Optional
                    .of(pregunta(Q1_VENDE, "SELLS_PRODUCTS", AnswerType.SINGLE, null, true)));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            ConfiguratorQuestionDto dto = service.execute(comando(Q1_VENDE, null));

            ArgumentCaptor<ConfiguratorQuestion> guardada = ArgumentCaptor
                    .forClass(ConfiguratorQuestion.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getCode()).isEqualTo("SELLS_PRODUCTS");
            assertThat(guardada.getValue().getQuestionText())
                    .isEqualTo("¿Vende productos al mostrador?");
            assertThat(guardada.getValue().getHelpText()).isEqualTo("ayuda");
            assertThat(guardada.getValue().isRequired()).isFalse();
            assertThat(guardada.getValue().getSortOrder()).isEqualTo(7);
            assertThat(dto.code()).isEqualTo("SELLS_PRODUCTS");

            verifyNoInteractions(optionRepository);
        }

        @Test
        @DisplayName("mover la pregunta a otra rama sana la guarda con el padre nuevo")
        void mover_la_pregunta_a_otra_rama_sana() {
            when(repository.findById(Q3_CUANTAS_CAJAS))
                    .thenReturn(Optional.of(pregunta(Q3_CUANTAS_CAJAS, "HOW_MANY",
                            AnswerType.NUMBER, O21_SI_MOSTRADOR, false)));
            when(optionRepository.findById(O11_SI_VENDE))
                    .thenReturn(Optional.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES")));
            when(optionRepository.findAllOrdered())
                    .thenReturn(List.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES")));
            when(repository.findAllOrdered()).thenReturn(
                    List.of(pregunta(Q1_VENDE, "SELLS", AnswerType.SINGLE, null, true)));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            ConfiguratorQuestionDto dto = service.execute(comando(Q3_CUANTAS_CAJAS, O11_SI_VENDE));

            assertThat(dto.parentOptionId()).isEqualTo(O11_SI_VENDE);
        }
    }

    @Nested
    @DisplayName("lo que corta antes de escribir")
    class Validaciones {

        @Test
        @DisplayName("una pregunta inexistente no comprueba la rama ni guarda")
        void una_pregunta_inexistente_no_guarda() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(99L, null)))
                    .isInstanceOf(ConfiguratorQuestionNotFoundException.class)
                    .hasMessageContaining("ConfiguratorQuestion not found: 99");

            verify(repository, never()).save(any());
            verifyNoInteractions(optionRepository);
        }

        @Test
        @DisplayName("colgar la pregunta de una opcion de su propia descendencia se rechaza y no la guarda a medias")
        void colgar_la_pregunta_de_su_propia_descendencia_se_rechaza() {
            ConfiguratorQuestion existente = pregunta(Q1_VENDE, "SELLS_PRODUCTS", AnswerType.SINGLE,
                    null, true);
            when(repository.findById(Q1_VENDE)).thenReturn(Optional.of(existente));
            when(optionRepository.findById(O21_SI_MOSTRADOR))
                    .thenReturn(Optional.of(opcion(O21_SI_MOSTRADOR, Q2_MOSTRADOR, "YES")));
            when(optionRepository.findAllOrdered())
                    .thenReturn(List.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES"),
                            opcion(O21_SI_MOSTRADOR, Q2_MOSTRADOR, "YES")));
            when(repository.findAllOrdered()).thenReturn(List.of(
                    pregunta(Q1_VENDE, "SELLS", AnswerType.SINGLE, null, true),
                    pregunta(Q2_MOSTRADOR, "COUNTER", AnswerType.SINGLE, O11_SI_VENDE, false)));

            assertThatThrownBy(() -> service.execute(comando(Q1_VENDE, O21_SI_MOSTRADOR)))
                    .isInstanceOf(ConditionalQuestionCycleException.class)
                    .hasMessageContaining("Conditional question cycle: question 1")
                    .hasMessageContaining("cannot depend on option 21");

            verify(repository, never()).save(any());
            assertThat(existente.getParentOptionId()).isNull();
            assertThat(existente.isRequired()).isTrue();
        }
    }

    /**
     * El mismo campo rompe dos invariantes en direcciones contrarias: salir de
     * {@code NUMBER} deja huérfanos los efectos {@code QUANTITY_FROM_ANSWER}, y
     * entrar en {@code NUMBER} deja vivas las opciones.
     */
    @Nested
    @DisplayName("el answerType y lo que ya cuelga de la pregunta")
    class CambioDeTipo {

        private static UpdateConfiguratorQuestionCommand aTipo(Long id, AnswerType tipo) {
            return new UpdateConfiguratorQuestionCommand(id, "¿Cuantas cajas?", null, tipo, null,
                    false, 3);
        }

        @Test
        @DisplayName("pasar a NUMBER una pregunta que ya tiene opciones se rechaza y no la guarda a medias")
        void pasar_a_number_con_opciones_vivas_se_rechaza() {
            // Sin esto responde 200 y deja las opciones colgando de una pregunta que
            // el asistente pinta como campo numerico. Se descubre semanas despues,
            // cuando un prospecto ya no puede terminar de cotizar.
            ConfiguratorQuestion existente = pregunta(Q1_VENDE, "SELLS_PRODUCTS", AnswerType.SINGLE,
                    null, true);
            when(repository.findById(Q1_VENDE)).thenReturn(Optional.of(existente));
            // effectRepository no se stubea: con nuevoTipo NUMBER, el guardian de
            // QUANTITY_FROM_ANSWER retorna antes de consultarlo.
            when(optionRepository.existsByQuestionId(Q1_VENDE)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(aTipo(Q1_VENDE, AnswerType.NUMBER)))
                    .isInstanceOf(NumberQuestionCannotHaveOptionsException.class)
                    .hasMessageContaining("Question 1 (SELLS_PRODUCTS)");

            verify(repository, never()).save(any());
            assertThat(existente.getAnswerType()).isEqualTo(AnswerType.SINGLE);
        }

        @Test
        @DisplayName("pasar a NUMBER una pregunta sin opciones se guarda")
        void pasar_a_number_sin_opciones_se_guarda() {
            when(repository.findById(Q1_VENDE)).thenReturn(Optional
                    .of(pregunta(Q1_VENDE, "SELLS_PRODUCTS", AnswerType.SINGLE, null, true)));
            when(optionRepository.existsByQuestionId(Q1_VENDE)).thenReturn(false);
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            ConfiguratorQuestionDto dto = service.execute(aTipo(Q1_VENDE, AnswerType.NUMBER));

            assertThat(dto.answerType()).isEqualTo(AnswerType.NUMBER);
        }

        @Test
        @DisplayName("devolver una NUMBER a SINGLE no consulta las opciones: es la salida de un cuestionario ya incoherente")
        void devolver_una_number_a_single_no_consulta_las_opciones() {
            when(repository.findById(Q3_CUANTAS_CAJAS)).thenReturn(Optional.of(
                    pregunta(Q3_CUANTAS_CAJAS, "HOW_MANY_BOXES", AnswerType.NUMBER, null, false)));
            when(effectRepository.existsQuantityFromAnswerByQuestionId(Q3_CUANTAS_CAJAS))
                    .thenReturn(false);
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            ConfiguratorQuestionDto dto = service
                    .execute(aTipo(Q3_CUANTAS_CAJAS, AnswerType.SINGLE));

            assertThat(dto.answerType()).isEqualTo(AnswerType.SINGLE);
            verify(optionRepository, never()).existsByQuestionId(any());
        }
    }
}
