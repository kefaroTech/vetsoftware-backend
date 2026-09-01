package com.vetsoftware.app.aiproposal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.aiproposal.application.command.RefineProposalCommand;
import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationRequest;
import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationResult;
import com.vetsoftware.app.aiproposal.application.dto.ProposalLineDto;
import com.vetsoftware.app.aiproposal.application.dto.ProposalViewDto;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalMetrics;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalRepository;
import com.vetsoftware.app.aiproposal.application.port.out.LegalConsentPort;
import com.vetsoftware.app.aiproposal.application.port.out.PaidInvocationSignalPort;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalGeneratorPort;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalLinkEmailSender;
import com.vetsoftware.app.aiproposal.application.port.out.SellableCatalogQueryPort;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.AiProposalNotFoundException;
import com.vetsoftware.app.aiproposal.domain.GenerationOutcome;
import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
import com.vetsoftware.app.aiproposal.domain.ProposalLine;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import com.vetsoftware.app.aiproposal.domain.ProposalTurn;
import com.vetsoftware.app.aiproposal.domain.ProposalVersionConflictException;
import com.vetsoftware.app.aiproposal.domain.ProspectText;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import com.vetsoftware.app.aiproposal.testsupport.ProposalMother;
import com.vetsoftware.app.aiproposal.testsupport.SellableCatalogMother;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El refinamiento: el tope de tres, los turnos acumulativos y la soberania de
 * la edicion manual.
 *
 * <p>
 * <b>El escenario es siempre el mismo prospecto</b>: pidio una propuesta, quito
 * "Caja" a mano y anadio "Laboratorio e imagen". Sobre ese estado se comprueba
 * que el modelo no puede deshacer ninguna de las dos decisiones.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefineProposalService — refinar sin deshacer lo que el cliente decidio")
class RefineProposalServiceTest {

    private static final Long TURNO_INICIAL = 71L;

    private static final Long TURNO_DE_EDICION = 72L;

    private static final Long TURNO_NUEVO = 73L;

    private static final String PRIMER_TEXTO = "somos una veterinaria de barrio en Chapinero";

    private static final String TEXTO_NUEVO = "tambien hacemos peluqueria canina";

    private final SellableCatalog catalog = SellableCatalogMother.sinPaquetes();

    @Mock
    private AiProposalRepository repository;

    @Mock
    private SellableCatalogQueryPort catalogQueryPort;

    @Mock
    private LegalConsentPort legalConsent;

    @Mock
    private ProposalGeneratorPort generator;

    @Mock
    private ProposalLinkEmailSender enlacePorCorreo;

    @Mock
    private AiProposalMetrics metrics;

    @Mock
    private PaidInvocationSignalPort paidInvocationSignal;

    private RefineProposalService service;

    @BeforeEach
    void montar() {
        service = new RefineProposalService(catalogQueryPort, generator,
                new ProposalTurnWriter(repository, legalConsent, enlacePorCorreo,
                        ProposalMother.RELOJ),
                new ProposalReader(repository, catalogQueryPort, ProposalMother.RELOJ), metrics,
                paidInvocationSignal, ProposalMother.MODELO, ProposalMother.PROMPT);
    }

    /** Ver el javadoc del mismo bloque en {@code GetProposalServiceTest}. */
    @Nested
    @DisplayName("Caducidad")
    class Caducidad {

        /**
         * &#9940; De las tres rutas, esta es la cara: sin la comprobacion, un token
         * caducado seguia comprando invocaciones de pago al modelo indefinidamente.
         */
        @Test
        @DisplayName("el refinamiento de una propuesta caducada es un 404 y NO llama al modelo")
        void el_refinamiento_de_una_propuesta_caducada_es_404() {
            when(repository.findByPublicToken(ProposalMother.TOKEN)).thenReturn(
                    Optional.of(ProposalMother.propuestaCaducada(ProposalMother.ID_PROPUESTA)));

            assertThatThrownBy(() -> service.refine(comando(null)))
                    .isInstanceOf(AiProposalNotFoundException.class);
            verifyNoInteractions(generator);
            verify(repository, never()).saveTurn(any());
        }
    }

    private RefineProposalCommand comando(Long version) {
        return new RefineProposalCommand(ProposalMother.TOKEN, TEXTO_NUEVO, version);
    }

    private AiProposal propuesta() {
        return ProposalMother.propuestaConVersion(ProposalMother.ID_PROPUESTA, 5L);
    }

    /**
     * El historial del prospecto: su primer texto y la edicion que hizo despues.
     */
    private List<ProposalTurn> historial() {
        return List.of(ProposalMother.turnoInicial(TURNO_INICIAL, PRIMER_TEXTO),
                ProposalMother.turnoDeEdicion(TURNO_DE_EDICION, 2));
    }

    /** Lo que dejo esa edicion: quito "Caja" y anadio "Laboratorio e imagen". */
    private List<ProposalLine> lineasDeLaEdicion() {
        return List.of(
                ProposalMother.lineaAnadidaPorElCliente(TURNO_DE_EDICION, "LAB_IMAGING", "45000.00",
                        0),
                ProposalMother.lineaPorCierre(TURNO_DE_EDICION, "CLINICAL_HISTORY", "49000.00", 1),
                ProposalMother.lineaPorCierre(TURNO_DE_EDICION, "SCHEDULING", "35000.00", 2),
                ProposalMother.lineaPorCierre(TURNO_DE_EDICION, "CORE", "69000.00", 3),
                ProposalMother.lineaRetiradaPorElCliente(TURNO_DE_EDICION, "CASH_REGISTER", 4));
    }

    /**
     * &#9888; El doble responde POR TURNO, no por un solo id: la fusion consulta
     * las lineas de {@code todos} los turnos para saber que quito y que anadio el
     * cliente, y un stub de un unico turno haria saltar la estrictez de Mockito en
     * el momento en que alguien anadiera un turno mas al historial.
     */
    private void conLineasPorTurno(Map<Long, List<ProposalLine>> porTurno) {
        when(repository.findLinesByTurnId(any())).thenAnswer(
                invocacion -> porTurno.getOrDefault(invocacion.getArgument(0), List.of()));
    }

    private void conPropuestaYaRefinable() {
        when(repository.findByPublicToken(ProposalMother.TOKEN))
                .thenReturn(Optional.of(propuesta()));
        when(repository.findTurnsByProposalId(ProposalMother.ID_PROPUESTA)).thenReturn(historial());
        when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA, ProposalBillingCycle.MONTHLY))
                .thenReturn(Optional.of(catalog));
        conLineasPorTurno(Map.of(TURNO_DE_EDICION, lineasDeLaEdicion()));
    }

    private void conEscrituraQueFunciona() {
        when(repository.saveTurn(any())).thenAnswer(invocacion -> {
            ProposalTurn turno = invocacion.getArgument(0);
            turno.setId(TURNO_NUEVO);
            return turno;
        });
        when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(catalogQueryPort.findItemIdsByCode()).thenReturn(ProposalMother.idsPorCodigo());
    }

    private List<String> codigosDeLaVista(ProposalViewDto vista) {
        return vista.lines().stream().map(ProposalLineDto::code).toList();
    }

    @Nested
    @DisplayName("Tope de refinamientos")
    class TopeDeRefinamientos {

        private List<ProposalTurn> turnosDeModelo(int cuantos) {
            List<ProposalTurn> turnos = new ArrayList<>();
            turnos.add(ProposalMother.turnoInicial(TURNO_INICIAL, PRIMER_TEXTO));
            java.util.stream.IntStream.rangeClosed(2, cuantos).forEach(numero -> turnos.add(
                    ProposalMother.turnoDeRefinamiento(TURNO_INICIAL + numero, numero, "y esto")));
            return List.copyOf(turnos);
        }

        @Test
        @DisplayName("el cuarto turno de modelo responde 200 con la propuesta intacta, nunca un"
                + " 400")
        void el_cuarto_turno_devuelve_la_propuesta_intacta() {
            when(repository.findByPublicToken(ProposalMother.TOKEN))
                    .thenReturn(Optional.of(propuesta()));
            when(repository.findTurnsByProposalId(ProposalMother.ID_PROPUESTA))
                    .thenReturn(turnosDeModelo(4));
            when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA,
                    ProposalBillingCycle.MONTHLY)).thenReturn(Optional.of(catalog));
            conLineasPorTurno(Map.of(TURNO_INICIAL,
                    List.of(ProposalMother.lineaDelModelo(TURNO_INICIAL, "CORE", "69000.00", 0))));

            ProposalViewDto vista = service.refine(comando(null));

            assertThat(vista.recalculated()).isFalse();
            assertThat(vista.refinementsLeft()).isZero();
            assertThat(codigosDeLaVista(vista)).containsExactly("CORE");
            verifyNoInteractions(generator);
            verify(repository, never()).saveTurn(any());
            verify(repository, never()).saveLines(any());
        }

        @Test
        @DisplayName("el tercer refinamiento todavia entra y sale con uno menos en el contador")
        void el_tercer_refinamiento_todavia_entra() {
            when(repository.findByPublicToken(ProposalMother.TOKEN))
                    .thenReturn(Optional.of(propuesta()));
            when(repository.findTurnsByProposalId(ProposalMother.ID_PROPUESTA))
                    .thenReturn(turnosDeModelo(3));
            when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA,
                    ProposalBillingCycle.MONTHLY)).thenReturn(Optional.of(catalog));
            conLineasPorTurno(Map.of(TURNO_INICIAL,
                    List.of(ProposalMother.lineaDelModelo(TURNO_INICIAL, "CORE", "69000.00", 0))));
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(
                    ProposalMother.exito(ProposalMother.borrador(List.of("CORE"), List.of())));

            ProposalViewDto vista = service.refine(comando(null));

            assertThat(vista.recalculated()).isTrue();
            assertThat(vista.refinementsLeft()).isEqualTo(1);
            verify(generator).generate(any());
        }
    }

    @Nested
    @DisplayName("Soberania de la edicion manual")
    class SoberaniaDeLaEdicion {

        @Test
        @DisplayName("lo que el cliente quito no vuelve aunque el modelo lo proponga otra vez")
        void lo_que_el_cliente_quito_no_vuelve() {
            conPropuestaYaRefinable();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(ProposalMother.exito(
                    ProposalMother.borrador(List.of("LAB_IMAGING", "CASH_REGISTER"), List.of())));

            ProposalViewDto vista = service.refine(comando(null));

            assertThat(codigosDeLaVista(vista)).contains("LAB_IMAGING")
                    .doesNotContain("CASH_REGISTER");
        }

        @Test
        @DisplayName("lo que el cliente anadio se conserva aunque el modelo lo omita")
        void lo_que_el_cliente_anadio_se_conserva() {
            conPropuestaYaRefinable();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(ProposalMother
                    .exito(ProposalMother.borrador(List.of("VACCINATION"), List.of())));

            ProposalViewDto vista = service.refine(comando(null));

            assertThat(codigosDeLaVista(vista)).contains("VACCINATION", "LAB_IMAGING");
        }

        @Test
        @DisplayName("un turno que no devuelve ni una linea no vacia el carrito del prospecto")
        void un_turno_sin_lineas_no_vacia_el_carrito() {
            conPropuestaYaRefinable();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(ProposalGenerationResult
                    .degradado(GenerationOutcome.DEGRADED_MODEL_UNAVAILABLE));

            ProposalViewDto vista = service.refine(comando(null));

            assertThat(codigosDeLaVista(vista))
                    .contains("LAB_IMAGING", "CLINICAL_HISTORY", "SCHEDULING", "CORE")
                    .doesNotContain("CASH_REGISTER");
            assertThat(vista.presentation()).isEqualTo(ProposalPresentation.DETERMINISTIC);
        }
    }

    @Nested
    @DisplayName("Turnos acumulativos")
    class TurnosAcumulativos {

        @Test
        @DisplayName("al modelo se le mandan los textos anteriores y el nuevo, en orden")
        void se_mandan_los_textos_anteriores_y_el_nuevo() {
            conPropuestaYaRefinable();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(
                    ProposalMother.exito(ProposalMother.borrador(List.of("CORE"), List.of())));

            service.refine(comando(null));

            ArgumentCaptor<ProposalGenerationRequest> peticion = ArgumentCaptor.captor();
            verify(generator).generate(peticion.capture());
            assertThat(peticion.getValue().customerTexts())
                    .containsExactly(ProspectText.of(PRIMER_TEXTO), ProspectText.of(TEXTO_NUEVO));
            assertThat(peticion.getValue().currentCartCodes()).contains("LAB_IMAGING");
        }

        @Test
        @DisplayName("el turno nuevo se numera detras del ultimo, contando la edicion manual")
        void el_turno_nuevo_se_numera_detras_del_ultimo() {
            conPropuestaYaRefinable();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(
                    ProposalMother.exito(ProposalMother.borrador(List.of("CORE"), List.of())));

            service.refine(comando(null));

            ArgumentCaptor<ProposalTurn> turno = ArgumentCaptor.captor();
            verify(repository, times(2)).saveTurn(turno.capture());
            assertThat(turno.getAllValues().getFirst().getTurnNumber()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un token desconocido no llega ni a mirar el catalogo")
        void un_token_desconocido_no_llega_al_catalogo() {
            when(repository.findByPublicToken(ProposalMother.TOKEN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.refine(comando(null)))
                    .isInstanceOf(AiProposalNotFoundException.class);
            verifyNoInteractions(generator, catalogQueryPort);
            verify(repository, never()).saveTurn(any());
        }

        @Test
        @DisplayName("una version vieja es un 409 y no escribe ni un turno")
        void una_version_vieja_no_escribe_nada() {
            when(repository.findByPublicToken(ProposalMother.TOKEN))
                    .thenReturn(Optional.of(propuesta()));

            assertThatThrownBy(() -> service.refine(comando(4L)))
                    .isInstanceOf(ProposalVersionConflictException.class);
            verifyNoInteractions(generator, catalogQueryPort);
            verify(repository, never()).saveTurn(any());
            verify(repository, never()).saveLines(any());
        }
    }

    /** Ver el javadoc del mismo bloque en {@code GenerateProposalServiceTest}. */
    @Nested
    @DisplayName("Respuesta degradada")
    class RespuestaDegradada {

        @Test
        @DisplayName("una degradacion responde de inmediato: no hay suelo de latencia que pagar")
        void una_degradacion_responde_de_inmediato() {
            conPropuestaYaRefinable();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(
                    ProposalGenerationResult.degradado(GenerationOutcome.DEGRADED_SPEND_CAP));

            long empezo = System.nanoTime();
            service.refine(comando(null));
            long transcurrido = (System.nanoTime() - empezo) / 1_000_000;

            assertThat(transcurrido).as("ms hasta responder una degradacion").isLessThan(1_000L);
        }

        @Test
        @DisplayName("la respuesta publica el estado degradado en presentation: por eso el suelo"
                + " de latencia no ocultaba nada")
        void la_respuesta_publica_el_estado_degradado() {
            conPropuestaYaRefinable();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(
                    ProposalGenerationResult.degradado(GenerationOutcome.DEGRADED_SPEND_CAP));

            assertThat(service.refine(comando(null)).presentation())
                    .isEqualTo(ProposalPresentation.DETERMINISTIC);
        }
    }

    /**
     * <b>El refinamiento sale del mismo presupuesto y paga lo mismo</b>: su
     * {@code RouteLimit} declara cupo diario por IP y comparte el cubo global con
     * la propuesta inicial. Asi que le aplica el mismo arreglo, y por los mismos
     * tres desenlaces: las degradaciones las emite el generador, que es comun.
     *
     * <p>
     * Lo que <b>no</b> le aplica es la pareja "sin catalogo / catalogo vacio": aqui
     * la tarifa ya esta elegida y persistida en la propuesta, y si dejara de leerse
     * {@code ProposalReader.catalogo} lanza —una excepcion, no un desenlace—, que
     * sin marca se cobra.
     */
    @Nested
    @DisplayName("Cupo diario: quien no invoca al modelo recupera su intento")
    class CupoDiario {

        @ParameterizedTest
        @CsvSource({"DEGRADED_SPEND_CAP", "DEGRADED_NO_HINTS", "DEGRADED_MODEL_UNAVAILABLE"})
        @DisplayName("las tres degradaciones devuelven el intento tambien al refinar")
        void las_tres_degradaciones_devuelven_el_intento(GenerationOutcome outcome) {
            conUnRefinamientoQueTermina(outcome);

            service.refine(comando(null));

            verify(paidInvocationSignal).signal(false);
        }

        @Test
        @DisplayName("un refinamiento correcto consume el intento")
        void un_refinamiento_correcto_consume_el_intento() {
            conUnRefinamientoQueTermina(GenerationOutcome.SUCCEEDED);

            service.refine(comando(null));

            verify(paidInvocationSignal).signal(true);
        }

        @Test
        @DisplayName("una invocacion FALLIDA consume igual: se pago lo mismo")
        void una_invocacion_fallida_consume_igual() {
            conUnRefinamientoQueTermina(GenerationOutcome.MODEL_FAILED);

            service.refine(comando(null));

            verify(paidInvocationSignal).signal(true);
        }

        private void conUnRefinamientoQueTermina(GenerationOutcome outcome) {
            conPropuestaYaRefinable();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(ProposalMother.resultadoDe(outcome,
                    ProposalMother.borrador(List.of("CORE"), List.of())));
        }
    }
}
