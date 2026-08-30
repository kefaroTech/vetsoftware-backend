package com.vetsoftware.app.aiproposal.application.usecase;

import com.vetsoftware.app.aiproposal.application.command.GenerateProposalCommand;
import com.vetsoftware.app.aiproposal.application.command.LegalAcceptanceCommand;
import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationRequest;
import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationResult;
import com.vetsoftware.app.aiproposal.application.dto.ProposalViewDto;
import com.vetsoftware.app.aiproposal.application.port.in.GenerateProposalUseCase;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalMetrics;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalMetrics.Operation;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalMetrics.ServedProposal;
import com.vetsoftware.app.aiproposal.application.port.out.LegalConsentPort;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalGeneratorPort;
import com.vetsoftware.app.aiproposal.application.port.out.ResponsePacingPort;
import com.vetsoftware.app.aiproposal.application.port.out.SellableCatalogQueryPort;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.CartResult;
import com.vetsoftware.app.aiproposal.domain.LegalDocumentVersionRef;
import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
import com.vetsoftware.app.aiproposal.domain.ProposalCart;
import com.vetsoftware.app.aiproposal.domain.ProposalDraft;
import com.vetsoftware.app.aiproposal.domain.ProposalToken;
import com.vetsoftware.app.aiproposal.domain.ProspectText;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * La propuesta inicial: <strong>TX1, la llamada fuera de transaccion,
 * TX2</strong>.
 *
 * <p>
 * &#9940; <strong>Esta clase NO lleva {@code @Transactional}, y no puede
 * llevarlo.</strong> {@code SIN_IO_EXTERNO_EN_TRANSACCION} se ensancho para
 * cazar exactamente esto: veta por prefijo los paquetes de los SDK de IA y
 * sigue la cadena de llamadas completa, asi que anotar aqui -o en cualquier
 * metodo que alcance a {@link ProposalGeneratorPort}- rompe el build. Y hace
 * bien: la invocacion tarda 3-8 segundos y retendria una conexion de Hikari y
 * sus locks durante todo ese tiempo. Las dos escrituras viven en
 * {@link ProposalTurnWriter}, que si es transaccional y no llama al modelo.
 *
 * <p>
 * <strong>La medida sale una sola vez y despues de TX2.</strong>
 * {@code writer.cerrarTurno} es quien lleva el {@code @Transactional}; este
 * metodo no, asi que cuando devuelve, la propuesta ya esta confirmada. Publicar
 * antes contaria propuestas que despues hacen rollback -que es el defecto que
 * {@code AfterCommitMetricRecorder} existe para evitar en los casos donde el
 * emisor si vive dentro de la transaccion-.
 */
@Service
@Observed(name = "aiproposal.generate", contextualName = "generate proposal")
public class GenerateProposalService implements GenerateProposalUseCase {

    /**
     * &#9940; Solo el ciclo mensual. La landing vende por mes y el ciclo anual
     * cotiza contra otra escalera de {@code catalog_prices}: dejarlo entrar por
     * parametro sin pantalla que lo elija seria un campo que nadie pone y que
     * cambia el precio.
     */
    private static final ProposalBillingCycle CICLO = ProposalBillingCycle.MONTHLY;

    private final SellableCatalogQueryPort catalogQueryPort;

    private final LegalConsentPort legalConsent;

    private final ProposalGeneratorPort generator;

    private final ProposalTurnWriter writer;

    private final ProposalReader reader;

    private final ResponsePacingPort pacing;

    private final AiProposalMetrics metrics;

    private final Clock clock;

    private final String modelId;

    private final String promptVersion;

    private final int diasDeVigencia;

    private final String locale;

    @SuppressWarnings("java:S107")
    public GenerateProposalService(SellableCatalogQueryPort catalogQueryPort,
            LegalConsentPort legalConsent, ProposalGeneratorPort generator,
            ProposalTurnWriter writer, ProposalReader reader, ResponsePacingPort pacing,
            AiProposalMetrics metrics, Clock clock,
            @Value("${vetsoftware.ai.proposal.model-id:anthropic.claude-sonnet-5}") String modelId,
            @Value("${vetsoftware.ai.proposal.prompt-version:v1}") String promptVersion,
            @Value("${vetsoftware.ai.proposal.validity-days:14}") int diasDeVigencia,
            @Value("${vetsoftware.ai.proposal.locale:es-CO}") String locale) {
        this.catalogQueryPort = catalogQueryPort;
        this.legalConsent = legalConsent;
        this.generator = generator;
        this.writer = writer;
        this.reader = reader;
        this.pacing = pacing;
        this.metrics = metrics;
        this.clock = clock;
        this.modelId = modelId;
        this.promptVersion = promptVersion;
        this.diasDeVigencia = diasDeVigencia;
        this.locale = locale;
    }

    @Override
    public ProposalViewDto generate(GenerateProposalCommand command) {
        Optional<AiProposal> yaVista = reader.porIdempotencia(command.contactEmail(),
                command.idempotencyKey());
        if (yaVista.isPresent())
            return reader.vista(yaVista.get(), false);

        ProspectText texto = ProspectText.of(command.description());

        Optional<Long> priceListId = catalogQueryPort.findPublishedPriceListId();
        if (priceListId.isEmpty())
            return sinCatalogo(texto);
        Optional<SellableCatalog> catalogo = catalogQueryPort.loadCatalog(priceListId.get(), CICLO);
        if (catalogo.isEmpty() || catalogo.get().items().isEmpty())
            return sinCatalogo(texto);
        SellableCatalog catalog = catalogo.get();

        List<LegalDocumentVersionRef> aceptadas = resolverConsentimiento(command.acceptances());
        Long avisoDePrivacidad = aceptadas.stream().filter(LegalDocumentVersionRef::privacyNotice)
                .map(LegalDocumentVersionRef::id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "the privacy notice acceptance is required"));

        ProposalTurnWriter.TurnoAbierto abierto = abrir(command, catalog, priceListId.get(),
                avisoDePrivacidad, aceptadas);
        if (abierto == null)
            return reader
                    .vista(reader.porIdempotencia(command.contactEmail(), command.idempotencyKey())
                            .orElseThrow(), false);

        long empezo = clock.millis();
        ProposalGenerationResult resultado = generator
                .generate(new ProposalGenerationRequest(List.of(texto), List.of(), catalog));

        ProposalDraft draft = resultado.draft();
        CartResult carrito = draft.outOfDomain()
                ? ProposalAssembler.vacio(catalog)
                : ProposalCart.build(draft.necessaryCodes(), draft.recommendedCodes(),
                        draft.textosDeMotivo(), catalog);

        AiProposal guardada = writer.cerrarTurno(abierto.proposal(), abierto.turn(), resultado,
                carrito, draft.contradictedCodes(), catalogQueryPort.findItemIdsByCode());
        ProposalPresentation presentacion = ProposalAssembler.presentacion(resultado.outcome(),
                draft);
        metrics.proposalServed(ServedProposal.de(Operation.PROPOSE, resultado.outcome(),
                presentacion, draft, carrito, texto.length(), guardada.getId()));

        // El suelo de latencia va DESPUES de escribir y ANTES de responder: lo que
        // se iguala es lo que el cliente mide, que es el tiempo hasta el ultimo byte.
        if (resultado.outcome().esDegradacionSinLlamada())
            pacing.applyDegradedFloor(clock.millis() - empezo);

        return reader.vista(guardada, carrito, catalog, presentacion, true);
    }

    /**
     * Sin tarifa publicada no hay nada que cotizar, y hasta hoy ese camino no
     * emitia <strong>ni una senal</strong>: el asistente respondia 200 con cero
     * lineas a todos los prospectos a la vez y la unica evidencia era que nadie
     * compraba. Se cuenta con {@code ai.outcome="no_catalog"}, que es el valor que
     * {@code GenerationOutcome} no puede tener porque el generador no llega a
     * verlo.
     */
    private ProposalViewDto sinCatalogo(ProspectText texto) {
        metrics.proposalServed(ServedProposal.sinCatalogo(Operation.PROPOSE, texto.length()));
        return ProposalViewDto.sinCatalogo();
    }

    /**
     * TX1, con la carrera de la idempotencia resuelta donde de verdad se decide.
     *
     * <p>
     * Dos peticiones simultaneas con la misma clave pasan las dos por la lectura de
     * arriba sin encontrar nada y llegan aqui; la segunda choca contra
     * {@code uq_ai_proposals_idempotency}. Devolver 500 seria castigar al usuario
     * por un doble clic que el propio boton de cancelar con {@code AbortController}
     * hace <em>mas</em> probable, no menos: se devuelve {@code null} y el llamante
     * relee la fila que gano.
     */
    private ProposalTurnWriter.TurnoAbierto abrir(GenerateProposalCommand command,
            SellableCatalog catalog, Long priceListId, Long avisoDePrivacidad,
            List<LegalDocumentVersionRef> aceptadas) {
        AiProposal nueva = AiProposal.create(ProposalToken.nuevo(), priceListId, CICLO,
                catalog.snapshotHash(), avisoDePrivacidad, command.idempotencyKey(),
                command.contactEmail(), locale, diasDeVigencia, clock);
        try {
            return writer.abrirPropuesta(nueva, command.description(), modelId, promptVersion,
                    command.idempotencyKey(), aceptadas, command.acceptedIpHash(),
                    command.userAgentHash());
        } catch (DataIntegrityViolationException carrera) {
            if (command.idempotencyKey() == null)
                throw carrera;
            return null;
        }
    }

    /**
     * &#9940; <strong>La aceptacion se guarda, no solo se valida.</strong> Una
     * casilla que se comprueba y no se persiste no es prueba de nada, y hasta el
     * changeset 387 este producto no tenia donde escribirla: guardaba que texto se
     * mostro, que es la mitad de la prueba y no la que exige el articulo 9 de la
     * Ley 1581.
     *
     * <p>
     * Se resuelve por el par {@code (code, documentVersion)} que manda el front, y
     * no por "la vigente ahora": si alguien publica una version entre que se pinta
     * la pantalla y se envia el formulario, la fila aceptada y la fila mostrada
     * dejarian de ser la misma y la evidencia probaria otra cosa. Un par que no
     * existe es un 400, no un consentimiento que se da por bueno.
     */
    private List<LegalDocumentVersionRef> resolverConsentimiento(
            List<LegalAcceptanceCommand> aceptaciones) {
        if (aceptaciones.isEmpty())
            throw new IllegalArgumentException("at least one legal acceptance is required");
        List<LegalDocumentVersionRef> resueltas = new ArrayList<>();
        for (LegalAcceptanceCommand aceptacion : aceptaciones) {
            resueltas.add(legalConsent.findVersion(aceptacion.code(), aceptacion.documentVersion())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "unknown legal document version: " + aceptacion.code())));
        }
        return resueltas;
    }
}
