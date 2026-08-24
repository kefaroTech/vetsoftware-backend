package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O21_SI_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q2_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q3_CUANTAS_CAJAS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.opcion;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.pregunta;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConditionalQuestionCycleException;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import com.vetsoftware.app.configurator.domain.ConfiguratorOptionNotFoundException;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El guardián vive fuera de los dos servicios que lo usan —alta y edición de
 * pregunta— porque escribir la comprobación dos veces es cómo una de las dos se
 * queda sin ella, y la edición es justamente el camino por el que un ciclo
 * entra.
 *
 * <p>
 * Árbol de referencia: {@code Q1 → O11 → Q2 → O21 → Q3}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConditionalQuestionGuard — el arco nuevo no puede cerrar un ciclo")
class ConditionalQuestionGuardTest {

    @Mock
    private ConfiguratorQuestionRepository questions;
    @Mock
    private ConfiguratorOptionRepository options;

    private static List<ConfiguratorOption> arbolDeOpciones() {
        return List.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES"),
                opcion(O21_SI_MOSTRADOR, Q2_MOSTRADOR, "YES"));
    }

    private static List<ConfiguratorQuestion> arbolDePreguntas() {
        return List.of(pregunta(Q1_VENDE, "SELLS_PRODUCTS", AnswerType.SINGLE, null, true),
                pregunta(Q2_MOSTRADOR, "HAS_COUNTER", AnswerType.SINGLE, O11_SI_VENDE, false),
                pregunta(Q3_CUANTAS_CAJAS, "HOW_MANY", AnswerType.NUMBER, O21_SI_MOSTRADOR, false));
    }

    @Nested
    @DisplayName("nada que comprobar")
    class SinPadre {

        @Test
        @DisplayName("dejar la pregunta en la raiz no toca la base")
        void dejar_la_pregunta_en_la_raiz_no_toca_la_base() {
            assertThatCode(() -> ConditionalQuestionGuard.assertParentIsUsable(Q3_CUANTAS_CAJAS,
                    null, questions, options)).doesNotThrowAnyException();

            verifyNoInteractions(questions, options);
        }
    }

    @Nested
    @DisplayName("la opcion padre tiene que existir")
    class Existencia {

        @Test
        @DisplayName("una opcion padre inexistente es un 404 con nombre, no un ciclo")
        void una_opcion_padre_inexistente_es_un_404() {
            when(options.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ConditionalQuestionGuard.assertParentIsUsable(Q3_CUANTAS_CAJAS,
                    999L, questions, options))
                    .isInstanceOf(ConfiguratorOptionNotFoundException.class)
                    .hasMessageContaining("ConfiguratorOption not found: 999");

            verifyNoInteractions(questions);
        }
    }

    @Nested
    @DisplayName("la topologia")
    class Topologia {

        @Test
        @DisplayName("colgar una pregunta nueva de una rama sana pasa")
        void colgar_una_pregunta_nueva_de_una_rama_sana_pasa() {
            when(options.findById(O21_SI_MOSTRADOR))
                    .thenReturn(Optional.of(opcion(O21_SI_MOSTRADOR, Q2_MOSTRADOR, "YES")));
            when(options.findAllOrdered()).thenReturn(arbolDeOpciones());
            when(questions.findAllOrdered()).thenReturn(arbolDePreguntas());

            assertThatCode(() -> ConditionalQuestionGuard.assertParentIsUsable(null,
                    O21_SI_MOSTRADOR, questions, options)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("colgar la raiz de una opcion de su descendencia se rechaza como ciclo")
        void colgar_la_raiz_de_su_propia_descendencia_se_rechaza() {
            when(options.findById(O21_SI_MOSTRADOR))
                    .thenReturn(Optional.of(opcion(O21_SI_MOSTRADOR, Q2_MOSTRADOR, "YES")));
            when(options.findAllOrdered()).thenReturn(arbolDeOpciones());
            when(questions.findAllOrdered()).thenReturn(arbolDePreguntas());

            assertThatThrownBy(() -> ConditionalQuestionGuard.assertParentIsUsable(Q1_VENDE,
                    O21_SI_MOSTRADOR, questions, options))
                    .isInstanceOf(ConditionalQuestionCycleException.class)
                    .hasMessageContaining("Conditional question cycle: question 1")
                    .hasMessageContaining("cannot depend on option 21");
        }

        @Test
        @DisplayName("colgar una pregunta de una opcion suya propia se rechaza")
        void colgar_una_pregunta_de_una_opcion_suya_propia_se_rechaza() {
            when(options.findById(O11_SI_VENDE))
                    .thenReturn(Optional.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES")));
            when(options.findAllOrdered()).thenReturn(arbolDeOpciones());
            when(questions.findAllOrdered()).thenReturn(arbolDePreguntas());

            assertThatThrownBy(() -> ConditionalQuestionGuard.assertParentIsUsable(Q1_VENDE,
                    O11_SI_VENDE, questions, options))
                    .isInstanceOf(ConditionalQuestionCycleException.class)
                    .hasMessageContaining("cannot depend on option 11");
        }
    }
}
