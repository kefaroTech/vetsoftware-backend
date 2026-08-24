package com.vetsoftware.app.configurator.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El cuestionario de estas pruebas es el ejemplo real del documento de diseño,
 * con dos niveles de condición:
 *
 * <pre>
 * Q1 SELLS_PRODUCTS (SINGLE, obligatoria, raiz)
 *    O11 YES  ──▶ Q2 HAS_COUNTER (SINGLE, condicional de O11)
 *    O12 NO             O21 YES ──▶ Q3 HOW_MANY_BOXES (NUMBER, condicional de O21)
 *                       O22 NO
 * </pre>
 *
 * Dos niveles y no uno a propósito: con un solo nivel, una comprobación que
 * solo mirase al padre inmediato pasaría todos los casos y la prueba no
 * distinguiría entre mirar un escalón y subir hasta la raíz.
 */
@DisplayName("ConfiguratorAnswerCoherence — que las respuestas encajen en el arbol")
class ConfiguratorAnswerCoherenceTest {

    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 8, 22, 10, 0);

    private static final Long Q1 = 1L;
    private static final Long Q2 = 2L;
    private static final Long Q3 = 3L;
    private static final Long O11_SI_VENDE = 11L;
    private static final Long O12_NO_VENDE = 12L;
    private static final Long O21_SI_MOSTRADOR = 21L;
    private static final Long O22_NO_MOSTRADOR = 22L;
    private static final Long Q4_SERVICIOS = 4L;
    private static final Long Q5_DOMICILIO = 5L;
    private static final Long O31_MUCHAS_CAJAS = 31L;
    private static final Long O41_PELUQUERIA = 41L;
    private static final Long O42_GUARDERIA = 42L;
    private static final Long O51_SI_DOMICILIO = 51L;
    private static final Long O52_NO_DOMICILIO = 52L;

    private static ConfiguratorQuestion pregunta(Long id, String code, AnswerType tipo,
            Long parentOptionId, boolean required) {
        return new ConfiguratorQuestion(id, code, "¿" + code + "?", null, tipo, parentOptionId,
                required, 0, CREADA, 0L, true);
    }

    private static ConfiguratorOption opcion(Long id, Long questionId, String code) {
        return new ConfiguratorOption(id, questionId, code, code, null, 0, CREADA, 0L, true);
    }

    /**
     * El cuestionario completo; {@code segundaObligatoria} es lo único que varía.
     */
    private static List<ConfiguratorQuestion> preguntas(boolean segundaObligatoria) {
        return List.of(pregunta(Q1, "SELLS_PRODUCTS", AnswerType.SINGLE, null, true),
                pregunta(Q2, "HAS_COUNTER", AnswerType.SINGLE, O11_SI_VENDE, segundaObligatoria),
                pregunta(Q3, "HOW_MANY_BOXES", AnswerType.NUMBER, O21_SI_MOSTRADOR, false));
    }

    private static List<ConfiguratorOption> opciones() {
        return List.of(opcion(O11_SI_VENDE, Q1, "YES"), opcion(O12_NO_VENDE, Q1, "NO"),
                opcion(O21_SI_MOSTRADOR, Q2, "YES"), opcion(O22_NO_MOSTRADOR, Q2, "NO"));
    }

    private static ConfiguratorAnswers respuestas(Set<Long> opciones, Map<Long, Integer> numeros) {
        return new ConfiguratorAnswers(opciones, numeros);
    }

    /**
     * El mismo árbol ampliado con los dos tipos que faltaban —una {@code MULTI} y
     * una {@code BOOLEAN}, las dos de raíz para que la alcanzabilidad no tape lo
     * que se quiere medir— y con una opción colgada por error de la pregunta
     * {@code NUMBER}, que es la fila que un cuestionario mal editado deja atrás.
     */
    private static List<ConfiguratorQuestion> preguntasConTodosLosTipos() {
        List<ConfiguratorQuestion> completas = new ArrayList<>(preguntas(false));
        completas.add(pregunta(Q4_SERVICIOS, "EXTRA_SERVICES", AnswerType.MULTI, null, false));
        completas.add(pregunta(Q5_DOMICILIO, "HOME_DELIVERY", AnswerType.BOOLEAN, null, false));
        return List.copyOf(completas);
    }

    private static List<ConfiguratorOption> opcionesConTodosLosTipos() {
        List<ConfiguratorOption> completas = new ArrayList<>(opciones());
        completas.add(opcion(O31_MUCHAS_CAJAS, Q3, "MANY"));
        completas.add(opcion(O41_PELUQUERIA, Q4_SERVICIOS, "GROOMING"));
        completas.add(opcion(O42_GUARDERIA, Q4_SERVICIOS, "BOARDING"));
        completas.add(opcion(O51_SI_DOMICILIO, Q5_DOMICILIO, "YES"));
        completas.add(opcion(O52_NO_DOMICILIO, Q5_DOMICILIO, "NO"));
        return List.copyOf(completas);
    }

    private static void assertCoherenteConTodosLosTipos(ConfiguratorAnswers dadas) {
        ConfiguratorAnswerCoherence.assertCoherent(preguntasConTodosLosTipos(),
                opcionesConTodosLosTipos(), dadas);
    }

    @Nested
    @DisplayName("nada sobra: respuestas de ramas que el cuestionario no activo")
    class RamasNoActivadas {

        @Test
        @DisplayName("una opcion cuya pregunta cuelga de otra opcion no marcada se rechaza")
        void una_opcion_de_rama_no_activada_se_rechaza() {
            // Responde solo la rama de mostrador sin haber dicho antes que vende:
            // el asistente nunca le habria enseñado esta pregunta.
            ConfiguratorAnswers manipuladas = respuestas(Set.of(O21_SI_MOSTRADOR), Map.of());

            assertThatThrownBy(() -> ConfiguratorAnswerCoherence.assertCoherent(preguntas(false),
                    opciones(), manipuladas)).isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("question 2").hasMessageContaining("HAS_COUNTER")
                    .hasMessageContaining("option 11");
        }

        @Test
        @DisplayName("una respuesta numerica de segundo nivel sin su primer nivel se rechaza")
        void una_respuesta_numerica_de_segundo_nivel_sin_su_primer_nivel_se_rechaza() {
            // Dice que vende, NO dice que cobre en mostrador, y aun asi manda el
            // numero de cajas. Un guardian que solo mirase al padre inmediato de Q3
            // veria O21 sin marcar y acertaria; el caso que de verdad separa mirar un
            // escalon de subir hasta la raiz es el de abajo.
            ConfiguratorAnswers manipuladas = respuestas(Set.of(O11_SI_VENDE), Map.of(Q3, 5));

            assertThatThrownBy(() -> ConfiguratorAnswerCoherence.assertCoherent(preguntas(false),
                    opciones(), manipuladas)).isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("question 3").hasMessageContaining("option 21");
        }

        @Test
        @DisplayName("marcar el segundo nivel saltandose el primero se rechaza aunque el padre inmediato si venga")
        void marcar_el_segundo_nivel_saltandose_el_primero_se_rechaza() {
            // O21 SI viene marcada, asi que el padre inmediato de Q3 esta satisfecho.
            // Lo que falta es O11, dos escalones mas arriba: sin subir hasta la raiz,
            // esta manipulacion pasa.
            ConfiguratorAnswers manipuladas = respuestas(Set.of(O21_SI_MOSTRADOR), Map.of(Q3, 5));

            assertThatThrownBy(() -> ConfiguratorAnswerCoherence.assertCoherent(preguntas(false),
                    opciones(), manipuladas)).isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("option 11");
        }

        @Test
        @DisplayName("una opcion que no existe en el cuestionario se rechaza, y con otro mensaje")
        void una_opcion_que_no_existe_se_rechaza() {
            ConfiguratorAnswers manipuladas = respuestas(Set.of(O11_SI_VENDE, 999L), Map.of());

            assertThatThrownBy(() -> ConfiguratorAnswerCoherence.assertCoherent(preguntas(false),
                    opciones(), manipuladas)).isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("option 999").hasMessageContaining("does not exist");
        }
    }

    @Nested
    @DisplayName("la rama activa de verdad se acepta")
    class RamasActivadas {

        @Test
        @DisplayName("el camino completo de dos niveles se acepta")
        void el_camino_completo_de_dos_niveles_se_acepta() {
            ConfiguratorAnswers coherentes = respuestas(Set.of(O11_SI_VENDE, O21_SI_MOSTRADOR),
                    Map.of(Q3, 5));

            assertThatCode(() -> ConfiguratorAnswerCoherence.assertCoherent(preguntas(false),
                    opciones(), coherentes)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("responder solo la raiz basta cuando lo condicional no es obligatorio")
        void responder_solo_la_raiz_basta() {
            ConfiguratorAnswers coherentes = respuestas(Set.of(O12_NO_VENDE), Map.of());

            assertThatCode(() -> ConfiguratorAnswerCoherence.assertCoherent(preguntas(false),
                    opciones(), coherentes)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("nada falta: obligatorias de ramas activas")
    class Obligatorias {

        @Test
        @DisplayName("una obligatoria de una rama activa sin responder se rechaza")
        void una_obligatoria_de_rama_activa_sin_responder_se_rechaza() {
            // Dice que vende —lo que activa Q2, aqui obligatoria— y calla. Sin esta
            // mitad, saltarse una pregunta impuesta no requiere añadir nada: basta
            // con omitir.
            ConfiguratorAnswers incompletas = respuestas(Set.of(O11_SI_VENDE), Map.of());

            assertThatThrownBy(() -> ConfiguratorAnswerCoherence.assertCoherent(preguntas(true),
                    opciones(), incompletas)).isInstanceOf(MissingRequiredAnswerException.class)
                    .hasMessageContaining("question 2").hasMessageContaining("HAS_COUNTER");
        }

        @Test
        @DisplayName("la misma obligatoria no se exige si su rama no esta activa")
        void la_misma_obligatoria_no_se_exige_si_su_rama_no_esta_activa() {
            ConfiguratorAnswers coherentes = respuestas(Set.of(O12_NO_VENDE), Map.of());

            assertThatCode(() -> ConfiguratorAnswerCoherence.assertCoherent(preguntas(true),
                    opciones(), coherentes)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("la obligatoria de la raiz sin responder se rechaza")
        void la_obligatoria_de_la_raiz_sin_responder_se_rechaza() {
            ConfiguratorAnswers vacias = respuestas(Set.of(), Map.of());

            assertThatThrownBy(() -> ConfiguratorAnswerCoherence.assertCoherent(preguntas(false),
                    opciones(), vacias)).isInstanceOf(MissingRequiredAnswerException.class)
                    .hasMessageContaining("question 1").hasMessageContaining("SELLS_PRODUCTS");
        }
    }

    /**
     * La tercera mitad: encajar en el árbol no basta si la respuesta viene en una
     * forma que la pregunta no admite. El daño no es un 500 sino una cotización: un
     * número aceptado por una pregunta que no es {@code NUMBER} es el que un efecto
     * {@code QUANTITY_FROM_ANSWER} huérfano convierte en cantidad de la línea.
     */
    @Nested
    @DisplayName("nada viene en la forma equivocada: el tipo de la respuesta")
    class TiposDeRespuesta {

        @Test
        @DisplayName("un numero para una pregunta SINGLE se rechaza — el caso que antes pasaba")
        void un_numero_para_una_pregunta_single_se_rechaza() {
            // Q1 es SINGLE y el asistente nunca pintó un campo numérico para ella.
            // Antes de esta comprobación el 9999 entraba sin decir nada y acababa
            // siendo la cantidad de la línea de cotización.
            ConfiguratorAnswers manipuladas = respuestas(Set.of(O12_NO_VENDE), Map.of(Q1, 9999));

            assertThatThrownBy(() -> assertCoherenteConTodosLosTipos(manipuladas))
                    .isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("question 1").hasMessageContaining("SELLS_PRODUCTS")
                    .hasMessageContaining("SINGLE")
                    .hasMessageContaining("never showed a numeric field");
        }

        @Test
        @DisplayName("un numero para una pregunta MULTI se rechaza")
        void un_numero_para_una_pregunta_multi_se_rechaza() {
            ConfiguratorAnswers manipuladas = respuestas(Set.of(O12_NO_VENDE),
                    Map.of(Q4_SERVICIOS, 7));

            assertThatThrownBy(() -> assertCoherenteConTodosLosTipos(manipuladas))
                    .isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("question 4").hasMessageContaining("MULTI");
        }

        @Test
        @DisplayName("un numero para una pregunta BOOLEAN se rechaza")
        void un_numero_para_una_pregunta_boolean_se_rechaza() {
            ConfiguratorAnswers manipuladas = respuestas(Set.of(O12_NO_VENDE),
                    Map.of(Q5_DOMICILIO, 3));

            assertThatThrownBy(() -> assertCoherenteConTodosLosTipos(manipuladas))
                    .isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("question 5").hasMessageContaining("BOOLEAN");
        }

        @Test
        @DisplayName("una opcion colgada de una pregunta NUMBER se rechaza — la simetrica")
        void una_opcion_de_una_pregunta_number_se_rechaza() {
            // O31 cuelga de Q3, que es NUMBER: una fila que solo puede existir por un
            // cuestionario mal editado. Marcarla dispararía sus efectos igual que
            // cualquier otra opción.
            ConfiguratorAnswers manipuladas = respuestas(
                    Set.of(O11_SI_VENDE, O21_SI_MOSTRADOR, O31_MUCHAS_CAJAS), Map.of());

            assertThatThrownBy(() -> assertCoherenteConTodosLosTipos(manipuladas))
                    .isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("question 3").hasMessageContaining("NUMBER")
                    .hasMessageContaining("option 31");
        }

        @Test
        @DisplayName("dos opciones de la misma pregunta SINGLE se rechazan")
        void dos_opciones_de_la_misma_pregunta_single_se_rechazan() {
            ConfiguratorAnswers manipuladas = respuestas(Set.of(O11_SI_VENDE, O12_NO_VENDE),
                    Map.of());

            assertThatThrownBy(() -> assertCoherenteConTodosLosTipos(manipuladas))
                    .isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("question 1")
                    .hasMessageContaining("admits a single answer")
                    .hasMessageContaining("options 11 and 12");
        }

        @Test
        @DisplayName("las dos opciones de una BOOLEAN a la vez se rechazan")
        void las_dos_opciones_de_una_boolean_se_rechazan() {
            ConfiguratorAnswers manipuladas = respuestas(
                    Set.of(O12_NO_VENDE, O51_SI_DOMICILIO, O52_NO_DOMICILIO), Map.of());

            assertThatThrownBy(() -> assertCoherenteConTodosLosTipos(manipuladas))
                    .isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("question 5").hasMessageContaining("BOOLEAN")
                    .hasMessageContaining("options 51 and 52");
        }

        @Test
        @DisplayName("marcar todas las opciones para activar todas las ramas se rechaza")
        void marcar_todas_las_opciones_se_rechaza() {
            // Sin la regla de cardinalidad esta es la vía que deja inútil la
            // comprobación de alcanzabilidad: marcando cada opción del cuestionario,
            // toda rama queda activa y todo efecto dispara.
            ConfiguratorAnswers manipuladas = respuestas(
                    Set.of(O11_SI_VENDE, O12_NO_VENDE, O21_SI_MOSTRADOR, O22_NO_MOSTRADOR,
                            O41_PELUQUERIA, O42_GUARDERIA, O51_SI_DOMICILIO, O52_NO_DOMICILIO),
                    Map.of());

            assertThatThrownBy(() -> assertCoherenteConTodosLosTipos(manipuladas))
                    .isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("admits a single answer");
        }

        @Test
        @DisplayName("varias opciones de una pregunta MULTI si se aceptan")
        void varias_opciones_de_una_multi_se_aceptan() {
            ConfiguratorAnswers coherentes = respuestas(
                    Set.of(O12_NO_VENDE, O41_PELUQUERIA, O42_GUARDERIA), Map.of());

            assertThatCode(() -> assertCoherenteConTodosLosTipos(coherentes))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("el numero de la pregunta NUMBER y una sola opcion por pregunta se aceptan")
        void el_camino_bien_tipado_se_acepta() {
            ConfiguratorAnswers coherentes = respuestas(
                    Set.of(O11_SI_VENDE, O21_SI_MOSTRADOR, O51_SI_DOMICILIO), Map.of(Q3, 5));

            assertThatCode(() -> assertCoherenteConTodosLosTipos(coherentes))
                    .doesNotThrowAnyException();
        }
    }
}
