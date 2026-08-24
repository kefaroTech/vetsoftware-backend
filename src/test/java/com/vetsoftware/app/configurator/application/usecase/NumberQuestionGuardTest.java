package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q3_CUANTAS_CAJAS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.pregunta;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.NumberQuestionCannotHaveOptionsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La comprobación cruza dos filas de dos tablas —la opción y el
 * {@code answer_type} de su pregunta— así que no la puede hacer ningún
 * {@code CHECK} de MySQL ni la invariante de {@code ConfiguratorOption}: la
 * entidad no conoce el tipo de respuesta de la pregunta a la que pertenece.
 *
 * <p>
 * Los dos extremos se prueban juntos porque son una sola invariante: sin el
 * segundo, la puerta que cierra el primero se abre desde el otro lado editando
 * la pregunta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NumberQuestionGuard — una pregunta NUMBER no tiene opciones")
class NumberQuestionGuardTest {

    @Mock
    private ConfiguratorOptionRepository options;

    @Nested
    @DisplayName("al crear la opcion: se mira el tipo de su pregunta")
    class AlCrearLaOpcion {

        @Test
        @DisplayName("colgar una opcion de una pregunta NUMBER se rechaza nombrando la pregunta")
        void colgar_una_opcion_de_una_pregunta_number_se_rechaza() {
            assertThatThrownBy(() -> NumberQuestionGuard.assertQuestionAdmitsOptions(
                    pregunta(Q3_CUANTAS_CAJAS, "HOW_MANY_BOXES", AnswerType.NUMBER, null, false)))
                    .isInstanceOf(NumberQuestionCannotHaveOptionsException.class)
                    .hasMessageContaining("Question 3 (HOW_MANY_BOXES)")
                    .hasMessageContaining("cannot be a NUMBER question and have options");
        }

        @ParameterizedTest(name = "answerType = {0}")
        @DisplayName("cualquier otro tipo si admite opciones")
        @EnumSource(value = AnswerType.class, names = "NUMBER", mode = EnumSource.Mode.EXCLUDE)
        void los_demas_tipos_admiten_opciones(AnswerType tipo) {
            assertThatCode(() -> NumberQuestionGuard.assertQuestionAdmitsOptions(
                    pregunta(Q1_VENDE, "SELLS_PRODUCTS", tipo, null, true)))
                    .doesNotThrowAnyException();

            verifyNoInteractions(options);
        }
    }

    @Nested
    @DisplayName("al editar la pregunta: se miran las opciones que ya tiene")
    class AlEditarLaPregunta {

        @Test
        @DisplayName("pasar a NUMBER una pregunta con opciones vivas se rechaza")
        void pasar_a_number_con_opciones_vivas_se_rechaza() {
            when(options.existsByQuestionId(Q1_VENDE)).thenReturn(true);

            assertThatThrownBy(() -> NumberQuestionGuard.assertNoOptionsInTheWay(Q1_VENDE,
                    "SELLS_PRODUCTS", AnswerType.NUMBER, options))
                    .isInstanceOf(NumberQuestionCannotHaveOptionsException.class)
                    .hasMessageContaining("Question 1 (SELLS_PRODUCTS)");
        }

        @Test
        @DisplayName("pasar a NUMBER una pregunta sin opciones pasa")
        void pasar_a_number_sin_opciones_pasa() {
            when(options.existsByQuestionId(Q1_VENDE)).thenReturn(false);

            assertThatCode(() -> NumberQuestionGuard.assertNoOptionsInTheWay(Q1_VENDE,
                    "SELLS_PRODUCTS", AnswerType.NUMBER, options)).doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "answerType = {0}")
        @DisplayName("volver a un tipo con opciones no consulta nada: es la salida de un cuestionario ya incoherente")
        @EnumSource(value = AnswerType.class, names = "NUMBER", mode = EnumSource.Mode.EXCLUDE)
        void volver_a_un_tipo_con_opciones_no_consulta_nada(AnswerType tipo) {
            // Deliberado: si una pregunta ya es NUMBER y arrastra opciones de antes,
            // devolverla a SINGLE es una de las dos formas de arreglarla. Un guardian
            // que cerrase tambien la vuelta dejaria esos datos sin salida por la API.
            assertThatCode(() -> NumberQuestionGuard.assertNoOptionsInTheWay(Q3_CUANTAS_CAJAS,
                    "HOW_MANY_BOXES", tipo, options)).doesNotThrowAnyException();

            verifyNoInteractions(options);
        }
    }
}
