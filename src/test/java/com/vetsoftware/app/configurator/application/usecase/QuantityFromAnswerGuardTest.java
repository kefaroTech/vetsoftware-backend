package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O21_SI_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
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
import com.vetsoftware.app.configurator.domain.ConfiguratorOptionNotFoundException;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionNotFoundException;
import com.vetsoftware.app.configurator.domain.EffectType;
import com.vetsoftware.app.configurator.domain.QuantityFromAnswerRequiresNumberQuestionException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La comprobación cruza dos filas de dos tablas —el efecto y la pregunta que lo
 * dispara— así que no la puede hacer ningún {@code CHECK} de MySQL ni la
 * invariante de {@code ConfiguratorEffect}: la entidad no conoce el tipo de
 * respuesta de su pregunta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuantityFromAnswerGuard — QUANTITY_FROM_ANSWER solo cuelga de una NUMBER")
class QuantityFromAnswerGuardTest {

    @Mock
    private ConfiguratorQuestionRepository questions;
    @Mock
    private ConfiguratorOptionRepository options;

    @Nested
    @DisplayName("los demas efectos ni se miran")
    class OtrosEfectos {

        @ParameterizedTest(name = "{0}")
        @DisplayName("no consulta nada para un efecto que no sea QUANTITY_FROM_ANSWER")
        @EnumSource(value = EffectType.class, names = "QUANTITY_FROM_ANSWER", mode = EnumSource.Mode.EXCLUDE)
        void no_consulta_nada_para_los_demas_efectos(EffectType tipo) {
            assertThatCode(() -> QuantityFromAnswerGuard.assertCoherent(tipo, O11_SI_VENDE, null,
                    questions, options)).doesNotThrowAnyException();

            verifyNoInteractions(questions, options);
        }
    }

    @Nested
    @DisplayName("disparado por pregunta")
    class PorPregunta {

        @Test
        @DisplayName("una pregunta NUMBER lo acepta")
        void una_pregunta_number_lo_acepta() {
            when(questions.findById(Q3_CUANTAS_CAJAS)).thenReturn(Optional
                    .of(pregunta(Q3_CUANTAS_CAJAS, "HOW_MANY", AnswerType.NUMBER, null, false)));

            assertThatCode(
                    () -> QuantityFromAnswerGuard.assertCoherent(EffectType.QUANTITY_FROM_ANSWER,
                            null, Q3_CUANTAS_CAJAS, questions, options))
                    .doesNotThrowAnyException();

            verifyNoInteractions(options);
        }

        @ParameterizedTest(name = "answerType = {0}")
        @DisplayName("cualquier tipo que no sea NUMBER se rechaza nombrando la pregunta y su tipo")
        @EnumSource(value = AnswerType.class, names = "NUMBER", mode = EnumSource.Mode.EXCLUDE)
        void un_tipo_que_no_es_number_se_rechaza(AnswerType tipo) {
            when(questions.findById(Q1_VENDE))
                    .thenReturn(Optional.of(pregunta(Q1_VENDE, "SELLS", tipo, null, true)));

            assertThatThrownBy(() -> QuantityFromAnswerGuard.assertCoherent(
                    EffectType.QUANTITY_FROM_ANSWER, null, Q1_VENDE, questions, options))
                    .isInstanceOf(QuantityFromAnswerRequiresNumberQuestionException.class)
                    .hasMessageContaining("QUANTITY_FROM_ANSWER requires a NUMBER question")
                    .hasMessageContaining("question 1 is " + tipo);
        }

        @Test
        @DisplayName("una pregunta que no existe da un 404 con nombre, no un NPE")
        void una_pregunta_que_no_existe_da_404() {
            when(questions.findById(Q3_CUANTAS_CAJAS)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> QuantityFromAnswerGuard.assertCoherent(
                    EffectType.QUANTITY_FROM_ANSWER, null, Q3_CUANTAS_CAJAS, questions, options))
                    .isInstanceOf(ConfiguratorQuestionNotFoundException.class)
                    .hasMessageContaining("ConfiguratorQuestion not found: 3");
        }
    }

    @Nested
    @DisplayName("disparado por opcion: sube por el disparador, sea cual sea")
    class PorOpcion {

        @Test
        @DisplayName("sube de la opcion a su pregunta y rechaza porque una pregunta con opciones no es NUMBER")
        void sube_de_la_opcion_a_su_pregunta_y_rechaza() {
            when(options.findById(O21_SI_MOSTRADOR))
                    .thenReturn(Optional.of(opcion(O21_SI_MOSTRADOR, Q1_VENDE, "YES")));
            when(questions.findById(Q1_VENDE)).thenReturn(
                    Optional.of(pregunta(Q1_VENDE, "SELLS", AnswerType.SINGLE, null, true)));

            assertThatThrownBy(() -> QuantityFromAnswerGuard.assertCoherent(
                    EffectType.QUANTITY_FROM_ANSWER, O21_SI_MOSTRADOR, null, questions, options))
                    .isInstanceOf(QuantityFromAnswerRequiresNumberQuestionException.class)
                    .hasMessageContaining("question 1 is SINGLE");
        }

        @Test
        @DisplayName("una opcion que no existe da un 404 con nombre y no llega a preguntar por la pregunta")
        void una_opcion_que_no_existe_da_404() {
            when(options.findById(O21_SI_MOSTRADOR)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> QuantityFromAnswerGuard.assertCoherent(
                    EffectType.QUANTITY_FROM_ANSWER, O21_SI_MOSTRADOR, null, questions, options))
                    .isInstanceOf(ConfiguratorOptionNotFoundException.class)
                    .hasMessageContaining("ConfiguratorOption not found: 21");

            verifyNoInteractions(questions);
        }
    }
}
