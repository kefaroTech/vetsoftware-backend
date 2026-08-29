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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.configurator.application.command.ResolveConfiguratorSelectionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorSelectionDto;
import com.vetsoftware.app.configurator.application.dto.SelectedItemDto;
import com.vetsoftware.app.configurator.application.port.out.CapacityCeilingQueryPort;
import com.vetsoftware.app.configurator.application.port.out.CatalogItemDependencyQueryPort;
import com.vetsoftware.app.configurator.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.BillingCycle;
import com.vetsoftware.app.configurator.domain.CatalogItemRef;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import com.vetsoftware.app.configurator.domain.EffectType;
import com.vetsoftware.app.configurator.domain.MissingRequiredAnswerException;
import com.vetsoftware.app.configurator.domain.PublishedPriceListRef;
import com.vetsoftware.app.configurator.domain.UnreachableAnswerException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El eslabón donde la coherencia de las respuestas, la resolución del carrito y
 * <strong>la resta de lo ya incluido</strong> se encadenan.
 *
 * <p>
 * Lo que este test defiende es el <strong>orden</strong>: si se resolviera
 * antes de comprobar, un prospecto podría meter artículos de ramas que el
 * asistente nunca activó; y si no se restara el techo, la cotización cobraría
 * unidades que el contrato ya trae puestas.
 *
 * <p>
 * Árbol de referencia: {@code Q1 → O11 → Q2 → O21 → Q3(NUMBER)}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResolveConfiguratorSelectionService — de respuestas a seleccion cotizable")
class ResolveConfiguratorSelectionServiceTest {

    /** El dia no importa aqui salvo para decidir que tarifa rige. */
    private static final Clock RELOJ = Clock.fixed(
            LocalDate.of(2026, 8, 29).atStartOfDay(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    private static final Long TARIFA = 500L;

    /** Un modulo: se enciende, no tiene eje y no se le resta nada. */
    private static final String COD_POS = "SCHEDULING";

    /** Un contador facturable sobre el eje TERMINAL. */
    private static final String COD_CAJA = "EXTRA_TERMINAL";

    @Mock
    private ConfiguratorEffectRepository repository;
    @Mock
    private ConfiguratorQuestionRepository questionRepository;
    @Mock
    private ConfiguratorOptionRepository optionRepository;
    @Mock
    private CatalogItemQueryPort catalogItemQueryPort;
    @Mock
    private CapacityCeilingQueryPort capacityCeilingQueryPort;
    @Mock
    private CatalogItemDependencyQueryPort dependencyQueryPort;

    private ResolveConfiguratorSelectionService service;

    @BeforeEach
    void crearServicio() {
        service = new ResolveConfiguratorSelectionService(repository, questionRepository,
                optionRepository, catalogItemQueryPort, capacityCeilingQueryPort,
                dependencyQueryPort, RELOJ);
    }

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

    private static CatalogItemRef modulo(Long id, String code) {
        return new CatalogItemRef(id, code, null, false);
    }

    /** Contador NO nuclear: es al que se le resta el techo. */
    private static CatalogItemRef contador(Long id, String code, String eje) {
        return new CatalogItemRef(id, code, eje, false);
    }

    /** El catalogo traduce los ids a rotulos. */
    private void elCatalogoTraduce(CatalogItemRef... refs) {
        when(catalogItemQueryPort.findActiveByIds(any())).thenReturn(List.of(refs));
    }

    /** Hoy no rige ninguna tarifa: no hay techo que restar. */
    private void sinTarifaVigente() {
        when(capacityCeilingQueryPort.findPublishedPriceLists()).thenReturn(List.of());
    }

    /** Hoy rige una tarifa con estos techos por eje. */
    private void conTechos(BillingCycle ciclo, Map<String, Integer> techos) {
        when(capacityCeilingQueryPort.findPublishedPriceLists()).thenReturn(
                List.of(new PublishedPriceListRef(TARIFA, LocalDate.of(2026, 1, 1), null)));
        when(capacityCeilingQueryPort.findStructuralCeilingsByAxis(TARIFA, ciclo))
                .thenReturn(techos);
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
            elCatalogoTraduce(modulo(ITEM_POS, COD_POS), contador(ITEM_CAJA, COD_CAJA, "TERMINAL"));
            sinTarifaVigente();

            ConfiguratorSelectionDto seleccion = service.resolve(
                    new ResolveConfiguratorSelectionCommand(Set.of(O11_SI_VENDE, O21_SI_MOSTRADOR),
                            Map.of(Q3_CUANTAS_CAJAS, 4), "MONTHLY"));

            assertThat(seleccion.items()).containsExactly(new SelectedItemDto(COD_POS, 1),
                    new SelectedItemDto(COD_CAJA, 4));
        }

        @Test
        @DisplayName("responder cero cajas deja la seleccion sin la linea de cero unidades")
        void responder_cero_deja_la_seleccion_sin_esa_linea() {
            conCuestionarioCompleto();
            when(repository.findAllOrdered()).thenReturn(List.of(efectoPorPregunta(1L,
                    Q3_CUANTAS_CAJAS, ITEM_CAJA, EffectType.QUANTITY_FROM_ANSWER, null)));

            ConfiguratorSelectionDto seleccion = service.resolve(
                    new ResolveConfiguratorSelectionCommand(Set.of(O11_SI_VENDE, O21_SI_MOSTRADOR),
                            Map.of(Q3_CUANTAS_CAJAS, 0), "MONTHLY"));

            assertThat(seleccion.items()).isEmpty();
        }

        @Test
        @DisplayName("una rama corta valida devuelve seleccion vacia sin errores")
        void una_rama_corta_valida_devuelve_seleccion_vacia() {
            conCuestionarioCompleto();
            when(repository.findAllOrdered()).thenReturn(List.of());

            ConfiguratorSelectionDto seleccion = service
                    .resolve(new ResolveConfiguratorSelectionCommand(Set.of(O12_NO_VENDE), Map.of(),
                            "MONTHLY"));

            assertThat(seleccion.items()).isEmpty();
        }

        /**
         * Un efecto puede apuntar a un articulo retirado de la venta despues de
         * sembrarlo. No tiene rotulo que publicar y no puede entrar en el carrito: la
         * contratacion lo rechazaria y el front no sabria pintarlo.
         */
        @Test
        @DisplayName("un articulo que ya no esta activo se cae del carrito, no sale sin rotulo")
        void un_articulo_inactivo_se_cae_del_carrito() {
            conCuestionarioCompleto();
            when(repository.findAllOrdered()).thenReturn(
                    List.of(efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null),
                            efectoPorOpcion(2L, O11_SI_VENDE, ITEM_CAJA, EffectType.ADD, null)));
            elCatalogoTraduce(modulo(ITEM_POS, COD_POS));
            sinTarifaVigente();

            ConfiguratorSelectionDto seleccion = service
                    .resolve(new ResolveConfiguratorSelectionCommand(Set.of(O11_SI_VENDE), Map.of(),
                            "MONTHLY"));

            assertThat(seleccion.items()).containsExactly(new SelectedItemDto(COD_POS, 1));
        }

        /**
         * <b>La mitad amable de REQUIRES.</b> Nueve arcos llevaban desde el changeset
         * 309 sin que los aplicara nadie: un carrito con Facturacion Electronica y sin
         * Caja se resolvia tal cual. Ahora se completa, y lo anadido sale con su rotulo
         * como cualquier otra linea.
         */
        @Test
        @DisplayName("completa el carrito con lo que sus piezas necesitan para funcionar")
        void completa_el_carrito_con_sus_requisitos() {
            conCuestionarioCompleto();
            when(repository.findAllOrdered()).thenReturn(
                    List.of(efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null)));
            when(dependencyQueryPort.findRequiredByItemId())
                    .thenReturn(Map.of(ITEM_POS, Set.of(ITEM_CAJA)));
            elCatalogoTraduce(modulo(ITEM_POS, COD_POS), modulo(ITEM_CAJA, COD_CAJA));
            sinTarifaVigente();

            ConfiguratorSelectionDto seleccion = service
                    .resolve(new ResolveConfiguratorSelectionCommand(Set.of(O11_SI_VENDE), Map.of(),
                            "MONTHLY"));

            assertThat(seleccion.items()).containsExactly(new SelectedItemDto(COD_POS, 1),
                    new SelectedItemDto(COD_CAJA, 1));
        }
    }

    /**
     * La segunda de las «DOS REGLAS DE CODIGO QUE NO SON DATO» del changeset 312.
     * La primera —el orden por {@code priority}— ya estaba escrita; esta no, y por
     * eso Ana salia pagando dos cosas que su contrato ya incluye.
     */
    @Nested
    @DisplayName("resta del techo ya incluido")
    class RestaDeLoIncluido {

        private void conUnContadorRespondido() {
            conCuestionarioCompleto();
            when(repository.findAllOrdered()).thenReturn(List.of(efectoPorPregunta(1L,
                    Q3_CUANTAS_CAJAS, ITEM_CAJA, EffectType.QUANTITY_FROM_ANSWER, null)));
            elCatalogoTraduce(contador(ITEM_CAJA, COD_CAJA, "TERMINAL"));
        }

        private ConfiguratorSelectionDto resolver(int respuesta) {
            return resolver(respuesta, "MONTHLY");
        }

        private ConfiguratorSelectionDto resolver(int respuesta, String ciclo) {
            return service.resolve(
                    new ResolveConfiguratorSelectionCommand(Set.of(O11_SI_VENDE, O21_SI_MOSTRADOR),
                            Map.of(Q3_CUANTAS_CAJAS, respuesta), ciclo));
        }

        /**
         * <b>El techo del ciclo pedido, no el del mensual.</b> Esta prueba es la que se
         * pone roja si alguien vuelve a clavar {@code billing_cycle = 'MONTHLY'} en la
         * consulta del techo. Los dos ciclos declaran techos <em>distintos</em> a
         * proposito: con el mensual clavado, doce unidades saldrian 12 - 2 = 10 en vez
         * de 12 - 5 = 7, y las dos cifras serian plausibles.
         */
        @Test
        @DisplayName("el techo sale del ciclo pedido: en anual no se resta el techo mensual")
        void el_techo_sale_del_ciclo_pedido() {
            conUnContadorRespondido();
            when(capacityCeilingQueryPort.findPublishedPriceLists()).thenReturn(
                    List.of(new PublishedPriceListRef(TARIFA, LocalDate.of(2026, 1, 1), null)));
            when(capacityCeilingQueryPort.findStructuralCeilingsByAxis(TARIFA, BillingCycle.ANNUAL))
                    .thenReturn(Map.of("TERMINAL", 5));

            assertThat(resolver(12, "ANNUAL").items())
                    .containsExactly(new SelectedItemDto(COD_CAJA, 7));
        }

        /**
         * Un ciclo que no existe muere en el caso de uso, no en un valueOf sin dueno.
         */
        @Test
        @DisplayName("un ciclo desconocido se rechaza con un mensaje que lo nombra")
        void un_ciclo_desconocido_se_rechaza() {
            conCuestionarioCompleto();
            when(repository.findAllOrdered()).thenReturn(List.of(efectoPorPregunta(1L,
                    Q3_CUANTAS_CAJAS, ITEM_CAJA, EffectType.QUANTITY_FROM_ANSWER, null)));
            elCatalogoTraduce(contador(ITEM_CAJA, COD_CAJA, "TERMINAL"));

            assertThatThrownBy(() -> resolver(3, "SEMANAL"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown billingCycle: SEMANAL");
        }

        /** Ana: una caja, y su contrato ya trae una. Cero unidades facturables. */
        @Test
        @DisplayName("justo en el techo no genera ninguna unidad facturable")
        void justo_en_el_techo_no_genera_ninguna_unidad() {
            conUnContadorRespondido();
            conTechos(BillingCycle.MONTHLY, Map.of("TERMINAL", 1));

            assertThat(resolver(1).items()).isEmpty();
        }

        @Test
        @DisplayName("uno por encima del techo genera exactamente una unidad")
        void uno_por_encima_del_techo_genera_una_unidad() {
            conUnContadorRespondido();
            conTechos(BillingCycle.MONTHLY, Map.of("TERMINAL", 1));

            assertThat(resolver(2).items()).containsExactly(new SelectedItemDto(COD_CAJA, 1));
        }

        @Test
        @DisplayName("por debajo del techo tampoco genera nada: la resta no baja de cero")
        void por_debajo_del_techo_no_genera_nada() {
            conUnContadorRespondido();
            conTechos(BillingCycle.MONTHLY, Map.of("TERMINAL", 3));

            assertThat(resolver(1).items()).isEmpty();
        }

        /**
         * El ancla de D-66 con el techo que resolvio el dueno: {@code CAPACITY_USER}
         * trae {@code included_quantity = 1} y el contrato lo concede con
         * {@code min_quantity = 1}, luego el techo es 2. Quince personas son
         * <b>trece</b> unidades — 8 x 12.000 + 5 x 9.000 = 141.000, no 156.000.
         */
        @Test
        @DisplayName("quince personas con techo dos son trece unidades: el ancla de D-66")
        void quince_personas_con_techo_dos_son_trece_unidades() {
            conUnContadorRespondido();
            conTechos(BillingCycle.MONTHLY, Map.of("TERMINAL", 2));

            assertThat(resolver(15).items()).containsExactly(new SelectedItemDto(COD_CAJA, 13));
        }

        /**
         * Un eje sin articulo del nucleo tarifado no trae nada incluido, asi que no hay
         * nada que restar. Es el caso de {@code STORAGE_GB}, que se vende entero.
         */
        @Test
        @DisplayName("un eje sin techo declarado no resta nada")
        void un_eje_sin_techo_declarado_no_resta_nada() {
            conUnContadorRespondido();
            conTechos(BillingCycle.MONTHLY, Map.of("USER", 2));

            assertThat(resolver(3).items()).containsExactly(new SelectedItemDto(COD_CAJA, 3));
        }

        /**
         * Sin tarifa vigente no se inventa un techo: restar uno adivinado regalaria
         * unidades, y devolver el carrito vacio dejaria la portada sin propuesta por un
         * dato de configuracion.
         */
        @Test
        @DisplayName("sin tarifa vigente el carrito sale en crudo, no se inventa un techo")
        void sin_tarifa_vigente_el_carrito_sale_en_crudo() {
            conUnContadorRespondido();
            sinTarifaVigente();

            assertThat(resolver(3).items()).containsExactly(new SelectedItemDto(COD_CAJA, 3));
        }

        /** A un modulo no hay techo que restarle: es una casilla encendida. */
        @Test
        @DisplayName("a un modulo no se le resta el techo de ningun eje")
        void a_un_modulo_no_se_le_resta_nada() {
            conCuestionarioCompleto();
            when(repository.findAllOrdered()).thenReturn(
                    List.of(efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null)));
            elCatalogoTraduce(modulo(ITEM_POS, COD_POS));
            conTechos(BillingCycle.MONTHLY, Map.of("TERMINAL", 5));

            ConfiguratorSelectionDto seleccion = service
                    .resolve(new ResolveConfiguratorSelectionCommand(Set.of(O11_SI_VENDE), Map.of(),
                            "MONTHLY"));

            assertThat(seleccion.items()).containsExactly(new SelectedItemDto(COD_POS, 1));
        }
    }

    @Nested
    @DisplayName("la coherencia corta antes de resolver")
    class Coherencia {

        @Test
        @DisplayName("una opcion de una rama no activada se rechaza sin llegar a leer los efectos")
        void una_opcion_de_rama_no_activada_no_llega_a_los_efectos() {
            conCuestionarioCompleto();

            assertThatThrownBy(() -> service.resolve(new ResolveConfiguratorSelectionCommand(
                    Set.of(O21_SI_MOSTRADOR), Map.of(), "MONTHLY")))
                    .isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("is not reachable");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("una opcion que no existe en el cuestionario se rechaza con otro mensaje")
        void una_opcion_inexistente_se_rechaza_con_otro_mensaje() {
            conCuestionarioCompleto();

            assertThatThrownBy(() -> service.resolve(
                    new ResolveConfiguratorSelectionCommand(Set.of(999L), Map.of(), "MONTHLY")))
                    .isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("Answer refers to option 999")
                    .hasMessageContaining("does not exist in the questionnaire");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("dejar sin responder la obligatoria de la raiz se rechaza y no resuelve nada")
        void dejar_sin_responder_la_obligatoria_de_la_raiz_se_rechaza() {
            conCuestionarioCompleto();

            assertThatThrownBy(() -> service.resolve(
                    new ResolveConfiguratorSelectionCommand(Set.of(), Map.of(), "MONTHLY")))
                    .isInstanceOf(MissingRequiredAnswerException.class)
                    .hasMessageContaining("Required question 1 (SELLS_PRODUCTS)");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("una respuesta numerica negativa se rechaza antes de tocar el cuestionario")
        void una_respuesta_numerica_negativa_se_rechaza_antes_de_todo() {
            assertThatThrownBy(() -> service.resolve(new ResolveConfiguratorSelectionCommand(
                    Set.of(O11_SI_VENDE), Map.of(Q3_CUANTAS_CAJAS, -1), "MONTHLY")))
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
                    Set.of(O11_SI_VENDE), Map.of(Q1_VENDE, 9999), "MONTHLY")))
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
                    Set.of(O11_SI_VENDE, O12_NO_VENDE), Map.of(), "MONTHLY")))
                    .isInstanceOf(UnreachableAnswerException.class)
                    .hasMessageContaining("admits a single answer");

            verifyNoInteractions(repository);
        }
    }
}
