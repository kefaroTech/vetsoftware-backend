package com.vetsoftware.app.aiproposal.application.usecase;

import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationResult;
import com.vetsoftware.app.aiproposal.application.dto.ProposalLinkEmail;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalRepository;
import com.vetsoftware.app.aiproposal.application.port.out.LegalConsentPort;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalLinkEmailSender;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.CartLine;
import com.vetsoftware.app.aiproposal.domain.CartResult;
import com.vetsoftware.app.aiproposal.domain.LegalDocumentVersionRef;
import com.vetsoftware.app.aiproposal.domain.LineAction;
import com.vetsoftware.app.aiproposal.domain.LineSource;
import com.vetsoftware.app.aiproposal.domain.LineVerdict;
import com.vetsoftware.app.aiproposal.domain.ProposalLine;
import com.vetsoftware.app.aiproposal.domain.ProposalTurn;
import com.vetsoftware.app.aiproposal.domain.TurnType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Las dos transacciones de la generacion, <strong>y solo ellas</strong>.
 *
 * <p>
 * &#9940; <strong>Esta clase existe para que el caso de uso pueda no ser
 * transaccional.</strong> La secuencia que impone
 * {@code SIN_IO_EXTERNO_EN_TRANSACCION} es: TX1 escribe la cabecera, las
 * aceptaciones y el turno {@code PENDING} y <em>commitea</em>; se invoca al
 * modelo <em>fuera de toda transaccion</em>; TX2 cierra el turno y escribe las
 * lineas. Un solo {@code @Transactional} alrededor del conjunto retendria una
 * conexion de Hikari y sus locks durante los 3-8 segundos que tarda el modelo,
 * y con trafico eso tumba el backend entero. La regla sigue la cadena de
 * llamadas completa, asi que no basta con no anotar el metodo que llama: hay
 * que partir de verdad.
 *
 * <p>
 * <strong>Y por eso los metodos de aqui no llaman a nadie de
 * {@code infrastructure/ai}</strong>. Si alguno lo hiciera, el build se caeria
 * -y haria bien-.
 */
@Component
public class ProposalTurnWriter {

    /**
     * Motivo fijo de las lineas con las que el modelo se contradijo. No hace eco
     * del codigo recibido: eso seria el canal lateral de S6.5 escrito en prosa.
     */
    static final String MOTIVO_CONTRADICCION = "Descartada: el asistente declaro el negocio fuera"
            + " de dominio y aun asi propuso lineas.";

    private static final Logger log = LoggerFactory.getLogger(ProposalTurnWriter.class);

    private final AiProposalRepository repository;

    private final LegalConsentPort legalConsent;

    private final ProposalLinkEmailSender enlacePorCorreo;

    private final Clock clock;

    public ProposalTurnWriter(AiProposalRepository repository, LegalConsentPort legalConsent,
            ProposalLinkEmailSender enlacePorCorreo, Clock clock) {
        this.repository = repository;
        this.legalConsent = legalConsent;
        this.enlacePorCorreo = enlacePorCorreo;
        this.clock = clock;
    }

    /**
     * TX1 de la propuesta nueva: cabecera, evidencia de consentimiento y turno
     * {@code PENDING}.
     *
     * <p>
     * <strong>Las aceptaciones van en la misma transaccion que la cabecera</strong>
     * y no despues: una propuesta persistida cuya fila de aceptacion no llego a
     * escribirse es una recogida de datos sin autorizacion probable, que es peor
     * que no haber persistido nada.
     */
    @Transactional
    public TurnoAbierto abrirPropuesta(AiProposal nueva, String inputText, String modelId,
            String promptVersion, String clientRequestId, List<LegalDocumentVersionRef> aceptadas,
            String acceptedIpHash, String userAgentHash) {
        AiProposal guardada = repository.save(nueva);
        LocalDateTime ahora = LocalDateTime.now(clock);
        for (LegalDocumentVersionRef aceptada : aceptadas) {
            legalConsent.recordAcceptance(aceptada.id(), guardada.getId(), ahora, acceptedIpHash,
                    userAgentHash);
        }
        ProposalTurn turno = repository.saveTurn(ProposalTurn.pendienteDeModelo(guardada.getId(), 1,
                TurnType.MODEL_INITIAL, inputText, modelId, promptVersion, clientRequestId, clock));
        return new TurnoAbierto(guardada, turno);
    }

    /** TX1 del refinamiento: solo el turno, que la cabecera ya existe. */
    @Transactional
    public TurnoAbierto abrirRefinamiento(AiProposal proposal, int turnNumber, String inputText,
            String modelId, String promptVersion, String clientRequestId) {
        ProposalTurn turno = repository.saveTurn(ProposalTurn.pendienteDeModelo(proposal.getId(),
                turnNumber, TurnType.MODEL_REFINEMENT, inputText, modelId, promptVersion,
                clientRequestId, clock));
        return new TurnoAbierto(proposal, turno);
    }

    /**
     * TX2: cierra el turno con lo que devolvio el modelo y escribe las lineas.
     *
     * <p>
     * <strong>Se llama tambien cuando no hubo invocacion</strong> -tope de gasto,
     * palanca apagada, sin hints-: el turno queda {@code FAILED} con su codigo y el
     * carrito determinista se escribe igual. Un turno que nunca recibio respuesta
     * es un estado normal del sistema, no una anomalia.
     */
    @Transactional
    public AiProposal cerrarTurno(AiProposal proposal, ProposalTurn turno,
            ProposalGenerationResult resultado, CartResult carrito, List<String> contradichos,
            Map<String, Long> idsPorCodigo) {
        if (resultado.seInvocoAlModelo()) {
            turno.cerrarConExito(resultado.usage().inputTokens(), resultado.usage().outputTokens(),
                    resultado.usage().latencyMs(), resultado.usage().stopReason(),
                    resultado.usage().rawResponse(), clock);
        } else {
            turno.cerrarConFallo(codigoDeFallo(resultado), resultado.latencyMs(), clock);
        }
        repository.saveTurn(turno);
        repository.saveLines(lineas(turno.getId(), carrito, contradichos, idsPorCodigo));
        proposal.registrarTurno(tokens(resultado, true), tokens(resultado, false), clock);
        if (!carrito.aceptadas().isEmpty())
            proposal.marcarPropuesta(clock);
        AiProposal guardada = repository.save(proposal);
        enviarEnlaceTrasCommit(enlaceDe(guardada, turno, carrito));
        return guardada;
    }

    /**
     * El payload del correo, resuelto <strong>dentro</strong> de la transaccion.
     *
     * <p>
     * Despues del commit la conexion ya volvio al pool y cualquier {@code find}
     * abriria la suya, sumando latencia a una respuesta que el prospecto esta
     * mirando. Aqui no hace falta ninguna consulta: todo sale de la entidad que se
     * acaba de guardar.
     *
     * <p>
     * <strong>Tres condiciones, y las tres son decisiones:</strong>
     * <ul>
     * <li><strong>Solo el turno inicial.</strong> {@code cerrarTurno} lo llaman
     * tambien los refinamientos, y el enlace no cambia entre turnos: mandarlo otra
     * vez seria hasta tres correos por propuesta diciendo lo mismo. El cupo por
     * hora del adaptador es la segunda red, no la primera.</li>
     * <li><strong>Solo con destinatario.</strong> El correo es opcional en la
     * cabecera y una propuesta anonimizada lo tiene a {@code NULL}.</li>
     * <li><strong>Solo con carrito.</strong> Un enlace a una propuesta vacia -el
     * negocio quedo fuera de dominio, o el modelo no llego a responder- manda al
     * prospecto a una pantalla que no le dice nada.</li>
     * </ul>
     */
    private ProposalLinkEmail enlaceDe(AiProposal proposal, ProposalTurn turno,
            CartResult carrito) {
        if (turno.getTurnType() != TurnType.MODEL_INITIAL || carrito.aceptadas().isEmpty())
            return null;
        String destinatario = proposal.getContactEmail();
        if (destinatario == null || destinatario.isBlank())
            return null;
        return new ProposalLinkEmail(destinatario, proposal.getPublicToken(),
                proposal.getExpiresAt());
    }

    /**
     * &#9940; <strong>El correo sale DESPUES del commit, nunca dentro.</strong> Un
     * mensaje entregado no vuelve: si esta transaccion revirtiera en el flush -por
     * el chequeo de {@code @Version}, por un {@code CHECK} de las tablas o por el
     * commit del llamante externo con propagacion {@code REQUIRED}-, el prospecto
     * se quedaria con el enlace de una propuesta que no existe. Es BE-18 tal cual,
     * con una cita en vez de una propuesta.
     *
     * <p>
     * <strong>Metodo propio y clase anonima, las dos cosas a proposito.</strong>
     * {@code SIN_IO_EXTERNO_EN_TRANSACCION} se detiene en el primer metodo que
     * habla con {@code TransactionSynchronizationManager}; escrito como
     * <em>lambda</em>, el {@code afterCommit} le queda atribuido al metodo que lo
     * declara y da falso positivo.
     *
     * <p>
     * <strong>La rama de guarda no es defensiva.</strong> Sin transaccion activa
     * -un test unitario, un llamante sin {@code @Transactional}-
     * {@code registerSynchronization} lanza {@code IllegalStateException}, y perder
     * el correo por eso seria cambiar un problema por otro peor.
     *
     * <p>
     * <strong>Y el callback jamas lanza.</strong> Una excepcion en
     * {@code afterCommit} se propaga al llamante con la transaccion ya confirmada:
     * convertiria una propuesta correctamente guardada en un 500.
     */
    private void enviarEnlaceTrasCommit(ProposalLinkEmail enlace) {
        if (enlace == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            enlacePorCorreo.send(enlace);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    enlacePorCorreo.send(enlace);
                } catch (RuntimeException fallo) {
                    log.warn("No se pudo enviar el enlace de la propuesta: {}", fallo.getMessage());
                }
            }
        });
    }

    /**
     * El turno de edicion manual: nace cerrado, no consume ni un token y no lleva
     * ni {@code modelId} ni {@code rawResponse} -el arco exclusivo del
     * {@code CHECK} de la tabla-.
     */
    @Transactional
    public AiProposal escribirEdicion(AiProposal proposal, int turnNumber, CartResult carrito,
            List<String> retiradas, Map<String, Long> idsPorCodigo, String clientRequestId) {
        ProposalTurn turno = repository.saveTurn(ProposalTurn.edicionDelCliente(proposal.getId(),
                turnNumber, clientRequestId, clock));
        List<ProposalLine> lineas = new ArrayList<>(
                lineas(turno.getId(), carrito, List.of(), idsPorCodigo));
        int orden = lineas.size();
        for (String codigo : retiradas) {
            Long catalogItemId = idsPorCodigo.get(codigo);
            if (catalogItemId == null)
                continue;
            lineas.add(new ProposalLine(null, turno.getId(), codigo, catalogItemId,
                    LineAction.REMOVED, LineSource.CUSTOMER, LineVerdict.ACCEPTED, 1, null, null,
                    null, orden++, LocalDateTime.now(clock), null));
        }
        repository.saveLines(lineas);
        proposal.registrarTurno(0, 0, clock);
        return repository.save(proposal);
    }

    /**
     * Las lineas del carrito mas las de la contradiccion.
     *
     * <p>
     * &#9940; <strong>La contradiccion se escribe, no se cuenta y se tira.</strong>
     * Un modelo que declara el negocio fuera de dominio y a la vez propone ocho
     * modulos se esta contradiciendo, y el plan (S8.2.1) llama a eso "una senal de
     * calidad que hay que ver". Se persiste una linea por codigo con veredicto
     * {@code NOT_SELLABLE} -que es literalmente lo que el plan manda hacer con
     * ellos- y sin {@code catalog_item_id}, que
     * {@code chk_ai_proposal_lines_resolved} solo exige cuando el veredicto es
     * {@code ACCEPTED}. <strong>Ninguna de estas lineas se serializa
     * jamas.</strong>
     */
    private List<ProposalLine> lineas(Long turnId, CartResult carrito, List<String> contradichos,
            Map<String, Long> idsPorCodigo) {
        List<ProposalLine> lineas = new ArrayList<>();
        for (CartLine linea : carrito.lineas()) {
            lineas.add(ProposalLine.de(linea, turnId, idsPorCodigo.get(linea.code()), clock));
        }
        int orden = lineas.size();
        for (String codigo : contradichos) {
            lineas.add(new ProposalLine(null, turnId, codigo, null, LineAction.ADDED,
                    LineSource.MODEL, LineVerdict.NOT_SELLABLE, 1, null, MOTIVO_CONTRADICCION, null,
                    orden++, LocalDateTime.now(clock), null));
        }
        return lineas;
    }

    private static String codigoDeFallo(ProposalGenerationResult resultado) {
        return resultado.failureCode() == null || resultado.failureCode().isBlank()
                ? resultado.outcome().name()
                : resultado.failureCode();
    }

    private static int tokens(ProposalGenerationResult resultado, boolean entrada) {
        if (!resultado.seInvocoAlModelo())
            return 0;
        Integer valor = entrada
                ? resultado.usage().inputTokens()
                : resultado.usage().outputTokens();
        return valor == null ? 0 : valor;
    }

    /** La cabecera ya con id y el turno abierto, que es lo que TX2 necesita. */
    public record TurnoAbierto(AiProposal proposal, ProposalTurn turn) {
    }
}
