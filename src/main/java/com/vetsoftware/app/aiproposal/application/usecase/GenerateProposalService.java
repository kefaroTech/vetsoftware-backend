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
import com.vetsoftware.app.aiproposal.application.port.out.SellableCatalogQueryPort;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.CartResult;
import com.vetsoftware.app.aiproposal.domain.LegalDocumentVersionRef;
import com.vetsoftware.app.aiproposal.domain.ProposalCart;
import com.vetsoftware.app.aiproposal.domain.ProposalDraft;
import com.vetsoftware.app.aiproposal.domain.ProposalToken;
import com.vetsoftware.app.aiproposal.domain.ProspectText;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import io.micrometer.observation.annotation.Observed;
import com.vetsoftware.app.shared.ai.ModelPricing;
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
     * Cuantas veces relee el perdedor de la carrera antes de rendirse. Ver
     * {@link #esperarALaGanadora}.
     */
    private static final int RELECTURAS = 4;

    /** Lo que espera entre relecturas: el orden de magnitud de un commit. */
    private static final long ESPERA_ENTRE_RELECTURAS_MS = 25L;

    private final SellableCatalogQueryPort catalogQueryPort;

    private final LegalConsentPort legalConsent;

    private final ProposalGeneratorPort generator;

    private final ProposalTurnWriter writer;

    private final ProposalReader reader;

    private final AiProposalMetrics metrics;

    private final Clock clock;

    private final String modelId;

    private final String promptVersion;

    private final int diasDeVigencia;

    private final String locale;

    @SuppressWarnings("java:S107")
    public GenerateProposalService(SellableCatalogQueryPort catalogQueryPort,
            LegalConsentPort legalConsent, ProposalGeneratorPort generator,
            ProposalTurnWriter writer, ProposalReader reader, AiProposalMetrics metrics,
            Clock clock,
            @Value("${vetsoftware.ai.proposal.model-id:" + ModelPricing.MODELO_POR_DEFECTO
                    + "}") String modelId,
            @Value("${vetsoftware.ai.proposal.prompt-version:v1}") String promptVersion,
            @Value("${vetsoftware.ai.proposal.validity-days:14}") int diasDeVigencia,
            @Value("${vetsoftware.ai.proposal.locale:es-CO}") String locale) {
        this.catalogQueryPort = catalogQueryPort;
        this.legalConsent = legalConsent;
        this.generator = generator;
        this.writer = writer;
        this.reader = reader;
        this.metrics = metrics;
        this.clock = clock;
        this.modelId = modelId;
        this.promptVersion = promptVersion;
        this.diasDeVigencia = diasDeVigencia;
        this.locale = locale;
    }

    @Override
    public ProposalViewDto generate(GenerateProposalCommand command) {
        Optional<AiProposal> previa = reader.porIdempotencia(command.contactEmail(),
                command.idempotencyKey());
        if (previa.isPresent() && previa.get().getBillingCycle() == command.billingCycle())
            return reader.vista(previa.get(), false);
        String clave = claveUtilizable(command, previa);

        ProspectText texto = ProspectText.of(command.description());

        Optional<Long> priceListId = catalogQueryPort.findPublishedPriceListId();
        if (priceListId.isEmpty())
            return sinCotizar(ServedProposal.sinCatalogo(Operation.PROPOSE, texto.length()));
        Optional<SellableCatalog> catalogo = catalogQueryPort.loadCatalog(priceListId.get(),
                command.billingCycle());
        if (catalogo.isEmpty() || catalogo.get().items().isEmpty())
            return sinCotizar(ServedProposal.catalogoVacio(Operation.PROPOSE, texto.length()));
        SellableCatalog catalog = catalogo.get();

        List<LegalDocumentVersionRef> aceptadas = resolverConsentimiento(command.acceptances());
        Long avisoDePrivacidad = aceptadas.stream().filter(LegalDocumentVersionRef::privacyNotice)
                .map(LegalDocumentVersionRef::id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "the privacy notice acceptance is required"));

        ProposalTurnWriter.TurnoAbierto abierto;
        try {
            abierto = abrir(command, clave, catalog, priceListId.get(), avisoDePrivacidad,
                    aceptadas);
        } catch (DataIntegrityViolationException carrera) {
            if (clave == null)
                throw carrera;
            return reader.vista(esperarALaGanadora(command, carrera), false);
        }

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
        metrics.proposalServed(ServedProposal.de(Operation.PROPOSE, resultado, presentacion,
                carrito, texto.length(), guardada.getId()));

        // Aqui vivia el suelo de latencia aleatorio de la ruta degradada. Se retiro:
        // el bit que ocultaba lo publica la respuesta. Ver
        // ProposalAssembler.presentacion.
        return reader.vista(guardada, carrito, catalog, presentacion, true);
    }

    /**
     * Los dos caminos en los que <strong>no hay nada que cotizar</strong>, y hasta
     * hoy ninguno emitia <strong>ni una senal</strong>: el asistente respondia 200
     * con cero lineas a todos los prospectos a la vez y la unica evidencia era que
     * nadie compraba. Se cuentan con los valores que {@code GenerationOutcome} no
     * puede tener, porque el generador no llega a verlos.
     *
     * <p>
     * &#9940; <strong>Son DOS desenlaces y no uno, aunque el prospecto vea lo
     * mismo.</strong> {@code no_catalog} es «no hay lista de precios
     * {@code PUBLISHED} vigente» —hay que publicar la tarifa—; {@code
     * empty_catalog} es «la lista esta publicada y no cuelga de ella ni un articulo
     * vendible» —la tarifa ya esta bien y hay que mirar el catalogo—. Colapsados en
     * un valor, la alerta mandaba a publicar una tarifa que ya estaba publicada.
     *
     * <p>
     * La presentacion es {@code NO_CATALOG} en los dos: describe lo que el
     * prospecto ve —nada—, y quien dice que hacer es el {@code outcome}.
     */
    private ProposalViewDto sinCotizar(ServedProposal medida) {
        metrics.proposalServed(medida);
        return ProposalViewDto.sinCatalogo();
    }

    /**
     * TX1. Dos peticiones simultaneas con la misma clave pasan las dos por la
     * lectura de arriba sin encontrar nada y llegan aqui; la segunda choca contra
     * {@code uq_ai_proposals_idempotency} y la excepcion sube al llamante, que la
     * resuelve en {@link #esperarALaGanadora}.
     */
    @SuppressWarnings("java:S107")
    private ProposalTurnWriter.TurnoAbierto abrir(GenerateProposalCommand command, String clave,
            SellableCatalog catalog, Long priceListId, Long avisoDePrivacidad,
            List<LegalDocumentVersionRef> aceptadas) {
        AiProposal nueva = AiProposal.create(ProposalToken.nuevo(), priceListId,
                command.billingCycle(), catalog.snapshotHash(), avisoDePrivacidad, clave,
                command.contactEmail(), locale, diasDeVigencia, clock);
        return writer.abrirPropuesta(nueva, command.description(), modelId, promptVersion, clave,
                aceptadas, command.acceptedIpHash(), command.userAgentHash());
    }

    /**
     * &#9940; <strong>La clave de idempotencia con la que se ESCRIBE, que no
     * siempre es la que mando el cliente.</strong>
     *
     * <p>
     * Sin esto, aceptar el ciclo por parametro no arregla el conmutador de la
     * pantalla: {@code Idempotency-Key} lo genera el front <em>al montar</em>, asi
     * que mensual y anual llegan con la MISMA clave. La lectura de arriba
     * encontraria la propuesta mensual ya escrita y la devolveria tal cual, y el
     * prospecto que acaba de pulsar "anual" seguiria viendo precio mensual — el
     * defecto exacto que este cambio existe para cerrar, movido tres lineas mas
     * abajo.
     *
     * <p>
     * <strong>Y no basta con no devolverla: hay que dejar de escribir con esa
     * clave.</strong> {@code uq_ai_proposals_idempotency} es
     * {@code (contact_email_hash, idempotency_key)} y la fila mensual ya la ocupa;
     * insertar la anual con la misma clave choca contra el unico, cae en
     * {@link #esperarALaGanadora} y devuelve <em>la mensual</em>, que es peor que
     * el defecto original porque ademas parece que funciono. Devolviendo
     * {@code null} la fila anual se escribe: InnoDB no cuenta {@code NULL} contra
     * si mismo en un indice {@code UNIQUE}.
     *
     * <p>
     * <strong>Lo que se paga, escrito:</strong> esa segunda propuesta pierde la
     * proteccion contra el doble clic. Es el mal menor y es acotado —solo la
     * peticion que cambia de ciclo, no las demas— y la alternativa seria un 409
     * sobre un conmutador de la interfaz, en un endpoint cuyo contrato es "siempre
     * 200". La solucion sin peaje es que el front emita una clave nueva al
     * conmutar, y entonces {@code previa} viene vacia y este metodo devuelve la
     * clave del cliente intacta.
     *
     * <p>
     * <strong>Limite declarado:</strong> dos pestanas que conmutan a la vez con la
     * misma clave siguen siendo una carrera, y {@link #esperarALaGanadora} devuelve
     * el ciclo del ganador. Es la ventana de un commit y no se trata aqui.
     */
    private static String claveUtilizable(GenerateProposalCommand command,
            Optional<AiProposal> previa) {
        return previa.isPresent() ? null : command.idempotencyKey();
    }

    /**
     * &#9940; <strong>El perdedor de la carrera espera a que el ganador commitee,
     * en vez de mirar una sola vez y reventar.</strong>
     *
     * <p>
     * Devolver 500 seria castigar al usuario por un doble clic que el propio boton
     * de cancelar con {@code AbortController} hace <em>mas</em> probable, no menos.
     * Pero releer <em>una</em> vez no cerraba el caso: entre el rechazo del indice
     * unico y el commit del ganador hay una ventana —corta y real— en la que la
     * fila todavia no es visible para nadie mas, y ahi el {@code orElseThrow}
     * convertia la carrera que se acababa de manejar en un
     * {@code NoSuchElementException}. Es el peor desenlace posible de los tres: el
     * ganador escribio, el perdedor no puede verlo <em>todavia</em>, y el que
     * recibe el 500 es quien hizo doble clic sobre una propuesta que si existe.
     *
     * <p>
     * <strong>Espera acotada y corta.</strong> {@link #RELECTURAS} intentos
     * separados por {@link #ESPERA_ENTRE_RELECTURAS_MS} milisegundos: el orden de
     * magnitud de un commit de una fila, no el de la invocacion al modelo. Si tras
     * el ultimo intento la fila sigue sin verse, <strong>se relanza la violacion
     * original</strong> y no un {@code NoSuchElementException}: el fallo que se
     * reporta es el que de verdad ocurrio, y la escritura que lo causo aparece en
     * el mensaje.
     *
     * <p>
     * <strong>Sin transaccion que retener.</strong> Este metodo se ejecuta fuera de
     * toda transaccion —esta clase no puede ser transaccional, ver arriba—, asi que
     * la espera no bloquea una conexion de Hikari ni ningun lock. Esa es la
     * condicion que hace admisible dormir aqui y que la haria inadmisible dentro de
     * {@link ProposalTurnWriter}.
     */
    private AiProposal esperarALaGanadora(GenerateProposalCommand command,
            DataIntegrityViolationException carrera) {
        for (int intento = 0; intento < RELECTURAS; intento++) {
            Optional<AiProposal> ganadora = reader.porIdempotencia(command.contactEmail(),
                    command.idempotencyKey());
            if (ganadora.isPresent())
                return ganadora.get();
            if (intento < RELECTURAS - 1)
                dormir();
        }
        throw carrera;
    }

    /**
     * La espera entre relecturas. Restaura el flag de interrupcion y abandona: si
     * alguien esta parando el hilo, insistir seria ignorarlo.
     */
    private static void dormir() {
        try {
            Thread.sleep(ESPERA_ENTRE_RELECTURAS_MS);
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
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
