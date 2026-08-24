package com.vetsoftware.app.configurator.domain;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O21_SI_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q2_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q3_CUANTAS_CAJAS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.opcion;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.pregunta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Un ciclo entre preguntas condicionales no da un error: da un cuestionario sin
 * raíz. El asistente no encuentra por dónde empezar y el prospecto se va, así
 * que la comprobación tiene que pasar al guardar, que es el único instante en
 * que el arco nuevo aún no existe.
 *
 * <p>
 * El árbol es el de referencia: {@code Q1 → O11 → Q2 → O21 → Q3}.
 */
@DisplayName("ConfiguratorQuestionTree — que el cuestionario siga siendo un arbol")
class ConfiguratorQuestionTreeTest {

    private static final Map<Long, Long> PREGUNTA_DE_LA_OPCION = Map.of(O11_SI_VENDE, Q1_VENDE,
            O21_SI_MOSTRADOR, Q2_MOSTRADOR);

    /** Q2 cuelga de O11 y Q3 de O21; Q1 es raíz y por eso no está en el mapa. */
    private static final Map<Long, Long> OPCION_PADRE_DE_LA_PREGUNTA = Map.of(Q2_MOSTRADOR,
            O11_SI_VENDE, Q3_CUANTAS_CAJAS, O21_SI_MOSTRADOR);

    @Nested
    @DisplayName("lo que no cierra ciclo pasa")
    class SinCiclo {

        @Test
        @DisplayName("dejar la pregunta en la raiz no comprueba nada")
        void dejar_la_pregunta_en_la_raiz_no_comprueba_nada() {
            assertThatCode(() -> ConfiguratorQuestionTree.assertNoCycle(Q3_CUANTAS_CAJAS, null,
                    Map.of(), Map.of())).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("colgar una pregunta nueva de una rama sana sube hasta la raiz y pasa")
        void colgar_una_pregunta_nueva_de_una_rama_sana_pasa() {
            assertThatCode(() -> ConfiguratorQuestionTree.assertNoCycle(null, O21_SI_MOSTRADOR,
                    PREGUNTA_DE_LA_OPCION, OPCION_PADRE_DE_LA_PREGUNTA)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("una opcion que ya no existe corta la subida sin acusar de ciclo")
        void una_opcion_inexistente_corta_la_subida_sin_acusar_de_ciclo() {
            assertThatCode(() -> ConfiguratorQuestionTree.assertNoCycle(Q1_VENDE, 999L,
                    PREGUNTA_DE_LA_OPCION, OPCION_PADRE_DE_LA_PREGUNTA)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("colgar Q3 de una opcion hermana de su rama no es ciclo")
        void colgar_de_una_opcion_hermana_no_es_ciclo() {
            assertThatCode(() -> ConfiguratorQuestionTree.assertNoCycle(Q3_CUANTAS_CAJAS,
                    O11_SI_VENDE, PREGUNTA_DE_LA_OPCION, OPCION_PADRE_DE_LA_PREGUNTA))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("lo que cierra ciclo se rechaza nombrando donde")
    class ConCiclo {

        @Test
        @DisplayName("colgar una pregunta de una opcion suya propia es un ciclo de un salto")
        void colgar_una_pregunta_de_una_opcion_suya_es_ciclo() {
            assertThatThrownBy(() -> ConfiguratorQuestionTree.assertNoCycle(Q1_VENDE, O11_SI_VENDE,
                    PREGUNTA_DE_LA_OPCION, OPCION_PADRE_DE_LA_PREGUNTA))
                    .isInstanceOf(ConditionalQuestionCycleException.class)
                    .hasMessageContaining("Conditional question cycle: question 1")
                    .hasMessageContaining("cannot depend on option 11");
        }

        @Test
        @DisplayName("colgar Q1 de una opcion de su nieta cierra el ciclo dos niveles mas arriba")
        void colgar_la_raiz_de_una_opcion_de_su_nieta_es_ciclo() {
            assertThatThrownBy(() -> ConfiguratorQuestionTree.assertNoCycle(Q1_VENDE,
                    O21_SI_MOSTRADOR, PREGUNTA_DE_LA_OPCION, OPCION_PADRE_DE_LA_PREGUNTA))
                    .isInstanceOf(ConditionalQuestionCycleException.class)
                    .hasMessageContaining("cannot depend on option 21");
        }

        @Test
        @DisplayName("un ciclo que ya estaba en los datos se rechaza aunque la pregunta sea nueva")
        void un_ciclo_preexistente_se_rechaza_aunque_la_pregunta_sea_nueva() {
            // Q1 cuelga de O21 y Q2 de O11: la ascendencia ya esta podrida antes de
            // guardar nada, y una pregunta nueva colgada de ahi nace inalcanzable.
            Map<Long, Long> podrido = Map.of(Q1_VENDE, O21_SI_MOSTRADOR, Q2_MOSTRADOR,
                    O11_SI_VENDE);

            assertThatThrownBy(() -> ConfiguratorQuestionTree.assertNoCycle(null, O11_SI_VENDE,
                    PREGUNTA_DE_LA_OPCION, podrido))
                    .isInstanceOf(ConditionalQuestionCycleException.class)
                    .hasMessageContaining("cycle already present above option 11")
                    .hasMessageContaining("is its own ancestor");
        }
    }

    @Nested
    @DisplayName("los mapas que alimentan la subida")
    class Mapas {

        @Test
        @DisplayName("questionIdByOptionId deja fuera las opciones todavia sin id")
        void question_id_by_option_id_deja_fuera_las_opciones_sin_id() {
            List<ConfiguratorOption> opciones = List.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES"),
                    new ConfiguratorOption(null, Q1_VENDE, "NO", "No", null, 1,
                            com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.CREADA_EL,
                            null, true));

            assertThat(ConfiguratorQuestionTree.questionIdByOptionId(opciones))
                    .containsExactlyEntriesOf(Map.of(O11_SI_VENDE, Q1_VENDE));
        }

        @Test
        @DisplayName("parentOptionIdByQuestionId omite las preguntas de raiz a proposito")
        void parent_option_id_by_question_id_omite_las_de_raiz() {
            List<ConfiguratorQuestion> preguntas = List.of(
                    pregunta(Q1_VENDE, "SELLS_PRODUCTS", AnswerType.SINGLE, null, true),
                    pregunta(Q2_MOSTRADOR, "HAS_COUNTER", AnswerType.SINGLE, O11_SI_VENDE, false));

            assertThat(ConfiguratorQuestionTree.parentOptionIdByQuestionId(preguntas))
                    .containsExactlyEntriesOf(Map.of(Q2_MOSTRADOR, O11_SI_VENDE));
        }

        @Test
        @DisplayName("una pregunta condicional todavia sin id tampoco entra en el mapa")
        void una_pregunta_condicional_sin_id_no_entra() {
            List<ConfiguratorQuestion> preguntas = List
                    .of(pregunta(null, "NUEVA", AnswerType.SINGLE, O11_SI_VENDE, false));

            assertThat(ConfiguratorQuestionTree.parentOptionIdByQuestionId(preguntas)).isEmpty();
        }
    }
}
