package com.vetsoftware.app.aiproposal.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.vetsoftware.app.aiproposal.application.command.GenerateProposalCommand;
import com.vetsoftware.app.aiproposal.application.command.LegalAcceptanceCommand;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalRepository;
import com.vetsoftware.app.aiproposal.application.port.out.CatalogHintQueryPort;
import com.vetsoftware.app.aiproposal.application.port.out.LegalConsentPort;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalLinkEmailSender;
import com.vetsoftware.app.aiproposal.application.port.out.SellableCatalogQueryPort;
import com.vetsoftware.app.aiproposal.application.port.out.SpendGuardPort;
import com.vetsoftware.app.aiproposal.application.port.out.SpendGuardPort.SpendReservation;
import com.vetsoftware.app.aiproposal.application.usecase.GenerateProposalService;
import com.vetsoftware.app.aiproposal.application.usecase.ProposalReader;
import com.vetsoftware.app.aiproposal.application.usecase.ProposalTurnWriter;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.LegalDocumentVersionRef;
import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
import com.vetsoftware.app.aiproposal.domain.ProposalTurn;
import com.vetsoftware.app.aiproposal.domain.ProspectText;
import com.vetsoftware.app.aiproposal.infrastructure.ai.BedrockProposalGenerator;
import com.vetsoftware.app.aiproposal.infrastructure.ai.ModelInvoker;
import com.vetsoftware.app.aiproposal.infrastructure.ai.ProposalPromptBuilder;
import com.vetsoftware.app.aiproposal.testsupport.ProposalMother;
import com.vetsoftware.app.aiproposal.testsupport.SellableCatalogMother;
import io.micrometer.common.KeyValue;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.KeyValuePair;

/**
 * <b>R1 del anexo B, ejercitada de punta a punta y con la mitad que
 * faltaba.</b>
 *
 * <p>
 * {@code ProspectTextTest} ya fijaba el mecanismo —{@code toString()} devuelve
 * la longitud— y comprobaba tres superficies: el mensaje, la concatenacion y el
 * MDC. Le faltaban dos, y las dos son por donde se filtra de verdad:
 *
 * <ul>
 * <li><b>Los atributos de un span.</b> Un span <em>no pasa por</em>
 * {@code RedactingAppender} —es un appender de Logback y las trazas salen por
 * OTLP directo—, asi que un {@code highCardinalityKeyValue} con el texto del
 * prospecto viaja en claro a Estados Unidos al 100 % de muestreo, sin que
 * ninguna prueba del repositorio se entere. Es la ruta que nadie recuerda.</li>
 * <li><b>La cadena de excepciones.</b> El mensaje de una excepcion de SDK puede
 * arrastrar el cuerpo de la peticion, y el cuerpo lleva el texto libre. Vale
 * igual para el log —{@code log.warn("...", e)}— y para el span
 * —{@code observation.error(e)}, que registra {@code exception.message} y
 * {@code exception.stacktrace} como atributos—.</li>
 * </ul>
 *
 * <p>
 * <b>Por que un valor senuelo y no una lista de campos prohibidos.</b> Una
 * revision de campos comprueba lo que alguien penso en mirar; un senuelo unico
 * comprueba <em>todas</em> las superficies a la vez, incluidas las que se
 * anadan manana. El senuelo entra dos veces por los dos caminos por los que
 * llega prosa ajena: lo escribe el prospecto y lo devuelve el modelo dentro del
 * motivo de una linea.
 *
 * <p>
 * <b>Y comprueba tambien lo contrario.</b> Una prueba que solo afirma ausencias
 * pasa igual de bien si no se emite nada: por eso afirma ademas que
 * {@code ai.input.chars} vale exactamente la longitud del texto. Eso es lo que
 * separa «no se filtra» de «no hay telemetria».
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("R1 — ni el texto del prospecto ni la prosa del modelo salen por log, MDC, campos, excepcion o span")
class AiProposalTelemetryLeakTest {

    /**
     * Improbable a proposito y en una sola pieza: si se partiera en palabras
     * corrientes, un {@code doesNotContain} podria fallar por una coincidencia
     * legitima y el test se acabaria relajando hasta no comprobar nada.
     */
    private static final String SENUELO = "Qx7ZtVeterinariaSanMarcosQx7Zt";

    private static final String DESCRIPCION = "Somos " + SENUELO
            + " de Chapinero, facturamos harto y atendemos perros y gatos";

    private static final Long ID_TURNO = 70L;

    private static final Map<String, String> HINTS = Map.of("CORE",
            "El nucleo: clientes y mascotas.", "CLINICAL_HISTORY", "Consultas y evolucion.");

    @Mock
    private AiProposalRepository repository;

    @Mock
    private SellableCatalogQueryPort catalogQueryPort;

    @Mock
    private LegalConsentPort legalConsent;

    @Mock
    private CatalogHintQueryPort hintQueryPort;

    @Mock
    private SpendGuardPort spendGuard;

    @Mock
    private ModelInvoker invoker;

    @Mock
    private ProposalLinkEmailSender enlacePorCorreo;

    private final List<Observation.Context> spans = new ArrayList<>();

    private ListAppender<ILoggingEvent> logs;

    private Logger raiz;

    private GenerateProposalService service;

    private ObservationRegistry observaciones;

    @BeforeEach
    void montar() {
        observaciones = ObservationRegistry.create();
        observaciones.observationConfig().observationHandler(new CapturadorDeSpans(spans));

        // El appender va en la RAIZ y no en el logger de una clase: la fuga que
        // esta prueba busca es la que escribe alguien que no sabe que existe esta
        // regla, y esa persona puede estar en cualquier clase de la cadena.
        raiz = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logs = new ListAppender<>();
        logs.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logs.start();
        raiz.addAppender(logs);

        BedrockProposalGenerator generator = new BedrockProposalGenerator(invoker,
                new ProposalPromptBuilder(), hintQueryPort, spendGuard, ProposalMother.RELOJ,
                observaciones);
        service = new GenerateProposalService(catalogQueryPort, legalConsent, generator,
                new ProposalTurnWriter(repository, legalConsent, enlacePorCorreo,
                        ProposalMother.RELOJ),
                new ProposalReader(repository, catalogQueryPort, ProposalMother.RELOJ),
                new MicrometerAiProposalMetrics(new SimpleMeterRegistry(), observaciones),
                ProposalMother.RELOJ, ProposalMother.MODELO, ProposalMother.PROMPT, 14, "es-CO");
    }

    @AfterEach
    void desmontar() {
        raiz.detachAppender(logs);
        logs.stop();
        MDC.clear();
    }

    @Test
    @DisplayName("camino feliz: el texto entra, el motivo del modelo vuelve con el senuelo y ninguna senal lo lleva")
    void el_camino_feliz_no_filtra_nada() {
        conTodoEnPie();
        when(invoker.isAvailable()).thenReturn(true);
        when(spendGuard.reserve(any()))
                .thenReturn(Optional.of(new SpendReservation("r-1", new BigDecimal("0.0176"))));
        // El modelo devuelve el senuelo DENTRO del motivo: es la segunda via por la
        // que entra prosa ajena, y la que el saneador convierte en un rechazo.
        when(invoker.invoke(any())).thenReturn(new ModelInvoker.ModelInvocation("claude-sonnet", """
                {"understood": true, "out_of_domain": false,
                 "necesarios": [{"code": "CORE", "motivo": "Para %s hace falta el nucleo"}],
                 "recomendados": []}
                """.formatted(SENUELO), 3200, 900, "end_turn"));

        observado(() -> service.generate(comando()));

        ningunaSenalLlevaElSenuelo();
        assertThat(atributoDeSpan("ai.input.chars"))
                .isEqualTo(String.valueOf(DESCRIPCION.length()));
    }

    @Test
    @DisplayName("el fallo del modelo tampoco: ni el mensaje de la excepcion ni el span lo llevan")
    void el_fallo_del_modelo_no_filtra_nada() {
        conTodoEnPie();
        when(invoker.isAvailable()).thenReturn(true);
        when(spendGuard.reserve(any()))
                .thenReturn(Optional.of(new SpendReservation("r-1", new BigDecimal("0.0176"))));
        // Exactamente el escenario peligroso: un SDK que mete el cuerpo de la
        // peticion en el mensaje de su excepcion, y el cuerpo lleva el texto libre.
        when(invoker.invoke(any()))
                .thenThrow(new ModelInvoker.ModelInvocationException("MODEL_INVALID_REQUEST",
                        "400 Bad Request; body={\"prompt\":\"" + DESCRIPCION + "\"}"));

        observado(() -> service.generate(comando()));

        ningunaSenalLlevaElSenuelo();
        // Y la senal si existe: el span del intento sale marcado, no verde.
        assertThat(atributoDeSpan("error.type")).isEqualTo("invalid_request");
    }

    @Test
    @DisplayName("el codigo de fallo saneado no puede fabricar una segunda linea de log (ASVS V7.3.1)")
    void el_codigo_de_fallo_no_inyecta() {
        conTodoEnPie();
        when(invoker.isAvailable()).thenReturn(true);
        when(spendGuard.reserve(any()))
                .thenReturn(Optional.of(new SpendReservation("r-1", new BigDecimal("0.0176"))));
        when(invoker.invoke(any())).thenThrow(new ModelInvoker.ModelInvocationException(
                "OK\r\nlevel=INFO evento=todo_bien " + SENUELO, "irrelevante"));

        observado(() -> service.generate(comando()));

        ningunaSenalLlevaElSenuelo();
        assertThat(campoDeLog("ai.failure.code")).isEqualTo("_OTHER");
        assertThat(campoDeLog("ai.error.type")).isEqualTo("_other");
    }

    // ── La comprobacion, sobre las cinco superficies a la vez ───────────────────

    private void ningunaSenalLlevaElSenuelo() {
        assertThat(logs.list).isNotNull();
        for (ILoggingEvent evento : logs.list) {
            assertThat(evento.getFormattedMessage()).as("mensaje de log").doesNotContain(SENUELO);
            assertThat(evento.getMDCPropertyMap().values()).as("valores del MDC")
                    .noneMatch(valor -> valor != null && valor.contains(SENUELO));
            assertThat(evento.getKeyValuePairs() == null
                    ? List.<KeyValuePair>of()
                    : evento.getKeyValuePairs()).as("pares clave-valor del evento")
                    .noneMatch(par -> String.valueOf(par.value).contains(SENUELO));
            assertThat(cadenaDeExcepciones(evento.getThrowableProxy())).as("cadena de excepciones")
                    .doesNotContain(SENUELO);
        }
        for (Observation.Context span : spans) {
            assertThat(span.getName()).as("nombre del span").doesNotContain(SENUELO);
            assertThat(String.valueOf(span.getContextualName())).as("nombre contextual del span")
                    .doesNotContain(SENUELO);
            assertThat(span.getAllKeyValues()).as("atributos del span %s", span.getName())
                    .noneMatch(
                            kv -> kv.getKey().contains(SENUELO) || kv.getValue().contains(SENUELO));
            assertThat(cadenaDeExcepciones(span.getError())).as("excepcion registrada en el span")
                    .doesNotContain(SENUELO);
        }
    }

    private static String cadenaDeExcepciones(IThrowableProxy proxy) {
        StringBuilder texto = new StringBuilder();
        for (IThrowableProxy actual = proxy; actual != null; actual = actual.getCause()) {
            texto.append(actual.getClassName()).append(' ').append(actual.getMessage()).append(' ');
        }
        return texto.toString();
    }

    private static String cadenaDeExcepciones(Throwable error) {
        StringBuilder texto = new StringBuilder();
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            texto.append(actual.getClass().getName()).append(' ').append(actual.getMessage())
                    .append(' ');
        }
        return texto.toString();
    }

    private String atributoDeSpan(String clave) {
        return spans.stream().flatMap(span -> span.getAllKeyValues().stream())
                .filter(kv -> kv.getKey().equals(clave)).map(KeyValue::getValue).findFirst()
                .orElse(null);
    }

    private String campoDeLog(String clave) {
        return logs.list.stream()
                .flatMap(evento -> evento.getKeyValuePairs() == null
                        ? List.<KeyValuePair>of().stream()
                        : evento.getKeyValuePairs().stream())
                .filter(par -> clave.equals(par.key)).map(par -> String.valueOf(par.value))
                .findFirst().orElse(null);
    }

    // ── Andamiaje ──────────────────────────────────────────────────────────────

    /**
     * Reproduce lo que hace el aspecto de {@code @Observed} sobre el caso de uso.
     * Sin este envoltorio no habria span padre y la prueba comprobaria la mitad.
     */
    private void observado(Runnable accion) {
        Observation.createNotStarted("aiproposal.generate", observaciones)
                .contextualName("generate proposal").observe(accion);
    }

    private void conTodoEnPie() {
        when(catalogQueryPort.findPublishedPriceListId())
                .thenReturn(Optional.of(ProposalMother.ID_TARIFA));
        when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA, ProposalBillingCycle.MONTHLY))
                .thenReturn(Optional.of(SellableCatalogMother.sinPaquetes()));
        when(legalConsent.findVersion("PRIVACY_NOTICE", 3)).thenReturn(Optional.of(
                new LegalDocumentVersionRef(ProposalMother.ID_AVISO, "PRIVACY_NOTICE", 3, true)));
        when(hintQueryPort.findCurrentHints()).thenReturn(HINTS);
        when(repository.save(any())).thenAnswer(invocacion -> {
            AiProposal propuesta = invocacion.getArgument(0);
            propuesta.setId(ProposalMother.ID_PROPUESTA);
            return propuesta;
        });
        when(repository.saveTurn(any())).thenAnswer(invocacion -> {
            ProposalTurn turno = invocacion.getArgument(0);
            turno.setId(ID_TURNO);
            return turno;
        });
        when(catalogQueryPort.findItemIdsByCode()).thenReturn(ProposalMother.idsPorCodigo());
        when(repository.findTurnsByProposalId(ProposalMother.ID_PROPUESTA))
                .thenReturn(List.of(ProposalMother.turnoInicial(ID_TURNO, DESCRIPCION)));
    }

    private static GenerateProposalCommand comando() {
        return new GenerateProposalCommand(ProposalMother.CORREO, DESCRIPCION, ProposalMother.CLAVE,
                List.of(new LegalAcceptanceCommand("PRIVACY_NOTICE", 3)), "iphash", "uahash",
                ProposalBillingCycle.MONTHLY);
    }

    /**
     * Guarda el contexto de cada observacion al cerrarse. Es lo que ve un
     * exportador de spans: nombre, nombre contextual, todos los pares clave-valor
     * -de baja <em>y</em> de alta cardinalidad- y el error registrado.
     */
    private record CapturadorDeSpans(List<Observation.Context> capturados)
            implements
                ObservationHandler<Observation.Context> {

        @Override
        public void onStop(Observation.Context context) {
            capturados.add(context);
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }
    }

    /**
     * Prueba de que el andamiaje no miente: si el senuelo estuviera en el texto y
     * {@link ProspectText} lo dejara salir, {@link #ningunaSenalLlevaElSenuelo()}
     * tiene que poder verlo. Se comprueba contra la unica superficie que esta
     * prueba controla del todo.
     */
    @Test
    @DisplayName("el senuelo SI se detecta si alguien lo escribe: el andamiaje no da falsos verdes")
    void el_andamiaje_detecta_una_fuga_real() {
        org.slf4j.Logger fugado = LoggerFactory.getLogger(AiProposalTelemetryLeakTest.class);
        fugado.warn("propuesta de {}", ProspectText.of(DESCRIPCION).revealForModelCall());

        assertThat(logs.list).anyMatch(evento -> evento.getFormattedMessage().contains(SENUELO));
    }
}
