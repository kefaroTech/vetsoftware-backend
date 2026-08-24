package com.vetsoftware.app.configurator.application.usecase;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_CAJA;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_POS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O12_NO_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O21_SI_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q1_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q2_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q3_CUANTAS_CAJAS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.efectoPorOpcion;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.efectoPorPregunta;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.opcion;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.pregunta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.command.ResolveConfiguratorSelectionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorSelectionDto;
import com.vetsoftware.app.configurator.application.dto.SelectedItemDto;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import com.vetsoftware.app.configurator.domain.EffectType;
import com.vetsoftware.app.configurator.domain.MissingRequiredAnswerException;
import com.vetsoftware.app.configurator.domain.UnreachableAnswerException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El eslabón donde la coherencia de las respuestas y la resolución del carrito
 * se encadenan. Lo que este test defiende es el <strong>orden</strong> de esos
 * dos pasos: si se resolviera antes de comprobar, un prospecto podría meter
 * artículos de ramas que el asistente nunca activó y la cotización saldría con
 * ellos.
 *
 * <p>
 * Árbol de referencia: {@code Q1 → O11 → Q2 → O21 → Q3(NUMBER)}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResolveConfiguratorSelectionService — de respuestas a seleccion cotizable")
class ResolveConfiguratorSelectionServiceTest {

    @Mock
    private ConfiguratorEffectRepository repository;
    @Mock
    private ConfiguratorQuestionRepository questionRepository;
    @Mock
    private ConfiguratorOptionRepository optionRepository;
    @InjectMocks
    private ResolveConfiguratorSelectionService service;

    private static List<ConfiguratorQuestion> cuestionario() {
        return List.of(pregunta(Q1_VENDE, "SELLS_PRODUCTS", AnswerType.SINGLE, null, true),
                pregunta(Q2_MOSTRADOR, "HAS_COUNTER", AnswerType.SINGLE, O11_SI_VENDE, false),
                pregunta(Q3_CUANTAS_CAJAS, "HOW_MANY_BOXES", AnswerType.NUMBER, O21_SI_MOSTRADOR,
                        false));
    }

    private static List<ConfiguratorOption> opciones() {
        return List.of(opcion(O11_SI_VENDE, Q1_VENDE, "YES"), opcion(O12_NO_VENDE, Q1_VENDE, "NO"),
                opcion(O21_SI_MOSTRADOR, Q2_MOSTRADOR, "YES"), opcion(22L, Q2_MOSTRADOR, "NO"));
    }

    private void conCuestionarioCompleto() {
        when(questionRepository.findAllOrdered()).thenReturn(cuestionario());
        when(optionRepository.findAllOrdered()).thenReturn(opciones());
    }

    @Nested
    @DisplayName("resolucion de una rama activa")
    class Resolucion {

        @Test
        @DisplayName("traduce el camino completo en articulos con la cantidad respondida")
        void traduce_el_camino_completo_en_articulos() {
            conCuestionarioCompleto();
            List<ConfiguratorEffect> efectos = List.of(
                    efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null),
                    efectoPorPregunta(2L, Q3_CUANTAS_CAJAS, ITEM_CAJA,
                            EffectType.QUANTITY_FROM_ANSWER, null));
            when(repository.findAllOrdered()).thenReturn(efectos);

            ConfiguratorSelectionDto seleccion = service.resolve(
                    new ResolveConfiguratorSelectionCommand(Set.of(O11_SI_VENDE, O21_SI_MOSTRADOR),
                            Map.of(Q3_CUANTAS_CAJAS, 4)));

            assertThat(seleccion.items()).containsExactly(new SelectedItemDto(ITEM_POS, 1),
                    new SelectedItemDto(ITEM_CAJA, 4));
        }

        @Test
        @DisplayName("responder cero cajas deja la seleccion sin la linea de cero unidades")
        void responder_cero_deja_la_seleccion_sin_esa_linea() {
            conCuestionarioCompleto();
            when(repository.findAllOrdered()).thenReturn(List.of(efectoPorPregunta(1L,
                    Q3_CUANTAS_CAJAS, ITEM_CAJA, EffectType.QUANTITY_FROM_ANSWER, null)));

            ConfiguratorSelectionDto seleccion = service.resolve(
                    new ResolveConfiguratorSelectionCommand(Set.of(O11_SI_VENDE, O21_SI_MOSTRADOR),
                            Map.of(Q3_CUANTAS_CAJAS, 0)));

            assertThat(seleccion.items()).isEmpty();
        }

        @Test
        @DisplayName("una rama corta valida devuelve seleccion vacia sin errores")
        void una_rama_corta_valida_devuelve_seleccion_vacia() {
            conCuestionarioCompleto();
            when(repository.findAllOrdered()).thenReturn(List.of());

            ConfiguratorSelectionDto seleccion = service.resolve(
                    new ResolveConfiguratorSelectionCommand(Set.of(O12_NO_VENDE), Map.of()));

            assertThat(seleccion.items()).isEmpty();
        }
    }

    @Nested
    @DisplayName("la coherencia corta antes de resolver")
    class Coherencia {

        @Test
        @DisplayName("una opcion de una rama no activada se rechaza sin llegar a leer los efectos")
        void una_opcion_de_rama_no_activada_no_llega_a_los_efectos() {
            conCuestionarioCompleto();

            assertThatThrownBy(() -> service.resolve(
                    new ResolveConfiguratorSelectionCommand(Set.of(O21_SI_MOSTRADOR), Map.of())))
                    .isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("is not reachable");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("una opcion que no existe en el cuestionario se rechaza con otro mensaje")
        void una_opcion_inexistente_se_rechaza_con_otro_mensaje() {
            conCuestionarioCompleto();

            assertThatThrownBy(() -> service
                    .resolve(new ResolveConfiguratorSelectionCommand(Set.of(999L), Map.of())))
                    .isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("Answer refers to option 999")
                    .hasMessageContaining("does not exist in the questionnaire");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("dejar sin responder la obligatoria de la raiz se rechaza y no resuelve nada")
        void dejar_sin_responder_la_obligatoria_de_la_raiz_se_rechaza() {
            conCuestionarioCompleto();

            assertThatThrownBy(() -> service
                    .resolve(new ResolveConfiguratorSelectionCommand(Set.of(), Map.of())))
                    .isInstanceOf(MissingRequiredAnswerException.class)
                    .hasMessageContaining("Required question 1 (SELLS_PRODUCTS)");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("una respuesta numerica negativa se rechaza antes de tocar el cuestionario")
        void una_respuesta_numerica_negativa_se_rechaza_antes_de_todo() {
            assertThatThrownBy(() -> service.resolve(new ResolveConfiguratorSelectionCommand(
                    Set.of(O11_SI_VENDE), Map.of(Q3_CUANTAS_CAJAS, -1))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be negative");

            verifyNoInteractions(repository, questionRepository, optionRepository);
        }

        @Test
        @DisplayName("un numero colado en una pregunta SINGLE no llega a los efectos")
        void un_numero_colado_en_una_pregunta_single_no_llega_a_los_efectos() {
            // El escenario del defecto: Q1 es SINGLE y aun así el cuerpo trae un
            // numericAnswers para ella. Si hubiera un QUANTITY_FROM_ANSWER huérfano
            // colgando de Q1 —de cuando la pregunta era NUMBER—, ese 9999 sería la
            // cantidad de la línea. Se corta antes de leer un solo efecto.
            conCuestionarioCompleto();

            assertThatThrownBy(() -> service.resolve(new ResolveConfiguratorSelectionCommand(
                    Set.of(O11_SI_VENDE), Map.of(Q1_VENDE, 9999))))
                    .isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("Answer to question 1 (SELLS_PRODUCTS)")
                    .hasMessageContaining("does not fit its answer type SINGLE");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("marcar las dos opciones de una SINGLE no llega a los efectos")
        void marcar_las_dos_opciones_de_una_single_no_llega_a_los_efectos() {
            // Marcar YES y NO a la vez activaría las dos ramas de Q1 y dispararía
            // los efectos de ambas: la alcanzabilidad, por sí sola, no lo impide.
            conCuestionarioCompleto();

            assertThatThrownBy(() -> service.resolve(new ResolveConfiguratorSelectionCommand(
                    Set.of(O11_SI_VENDE, O12_NO_VENDE), Map.of())))
                    .isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("admits a single answer");

            verifyNoInteractions(repository);
        }
    }
}
