package com.vetsoftware.app.aiproposal.application.usecase;

import com.vetsoftware.app.aiproposal.application.command.RefineProposalCommand;
import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationRequest;
import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationResult;
import com.vetsoftware.app.aiproposal.application.dto.ProposalViewDto;
import com.vetsoftware.app.aiproposal.application.port.in.RefineProposalUseCase;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalMetrics;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalMetrics.Operation;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalMetrics.ServedProposal;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalGeneratorPort;
import com.vetsoftware.app.aiproposal.application.port.out.SellableCatalogQueryPort;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.CartResult;
import com.vetsoftware.app.aiproposal.domain.ProposalCart;
import com.vetsoftware.app.aiproposal.domain.ProposalDraft;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import com.vetsoftware.app.aiproposal.domain.ProposalTurn;
import com.vetsoftware.app.aiproposal.domain.ProspectText;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import io.micrometer.observation.annotation.Observed;
import com.vetsoftware.app.shared.ai.ModelPricing;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * El refinamiento: mismo esquema de dos transacciones con la llamada en medio.
 *
 * <p>
 * <strong>Dos reglas que no son opcionales:</strong>
 *
 * <ul>
 * <li><strong>Los turnos son acumulativos.</strong> Al modelo se le mandan
 * todos los textos del cliente en orden, no solo el ultimo (S7.2.1).</li>
 * <li><strong>La edicion manual es soberana.</strong> Lo que el cliente quito
 * no se re-anade aunque el modelo lo vuelva a proponer, y lo que anadio se
 * conserva aunque el modelo lo omita (S8.3).</li>
 * </ul>
 *
 * <p>
 * <strong>Al cuarto turno de modelo se responde 200 con la propuesta
 * intacta</strong> y {@code recalculated = false}. Nunca un 400: la interfaz lo
 * leeria como una averia y el usuario no hizo nada mal.
 *
 * <p>
 * <strong>Ese cuarto turno NO se cuenta como propuesta servida</strong>, y es
 * deliberado: no se genero nada, no se gasto nada y no hay calidad de modelo
 * que medir. Contarlo mezclaria «cuantas propuestas produce el asistente» con
 * «cuantos prospectos agotan sus ajustes», que son dos preguntas distintas y la
 * segunda no se responde a las tres de la manana.
 */
@Service
@Observed(name = "aiproposal.refine", contextualName = "refine proposal")
public class RefineProposalService implements RefineProposalUseCase {

    private final SellableCatalogQueryPort catalogQueryPort;

    private final ProposalGeneratorPort generator;

    private final ProposalTurnWriter writer;

    private final ProposalReader reader;

    private final AiProposalMetrics metrics;

    private final String modelId;

    private final String promptVersion;

    @SuppressWarnings("java:S107")
    public RefineProposalService(SellableCatalogQueryPort catalogQueryPort,
            ProposalGeneratorPort generator, ProposalTurnWriter writer, ProposalReader reader,
            AiProposalMetrics metrics,
            @Value("${vetsoftware.ai.proposal.model-id:" + ModelPricing.MODELO_POR_DEFECTO
                    + "}") String modelId,
            @Value("${vetsoftware.ai.proposal.prompt-version:v1}") String promptVersion) {
        this.catalogQueryPort = catalogQueryPort;
        this.generator = generator;
        this.writer = writer;
        this.reader = reader;
        this.metrics = metrics;
        this.modelId = modelId;
        this.promptVersion = promptVersion;
    }

    @Override
    public ProposalViewDto refine(RefineProposalCommand command) {
        AiProposal proposal = reader.exigir(command.publicToken());
        reader.exigirVersion(proposal, command.expectedVersion());

        List<ProposalTurn> turnos = reader.turnos(proposal);
        if (reader.turnosDeModelo(turnos) >= ProposalReader.MAX_TURNOS_DE_MODELO)
            return reader.vista(proposal, false);

        SellableCatalog catalog = reader.catalogo(proposal);
        CartResult vigente = ProposalAssembler.reconstruir(reader.lineasVigentes(proposal),
                catalog);

        ProposalTurnWriter.TurnoAbierto abierto = writer.abrirRefinamiento(proposal,
                reader.siguienteNumeroDeTurno(turnos), command.text(), modelId, promptVersion,
                null);

        ProspectText texto = ProspectText.of(command.text());
        List<ProspectText> textos = new ArrayList<>(reader.textosDelCliente(turnos));
        textos.add(texto);

        ProposalGenerationResult resultado = generator.generate(
                new ProposalGenerationRequest(textos, reader.codigosAceptados(vigente), catalog));

        ProposalDraft draft = resultado.draft();
        CartResult carrito = draft.outOfDomain()
                ? ProposalAssembler.vacio(catalog)
                : ProposalCart.build(fusionar(draft, turnos, vigente), draft.recommendedCodes(),
                        draft.textosDeMotivo(), catalog);

        AiProposal guardada = writer.cerrarTurno(abierto.proposal(), abierto.turn(), resultado,
                carrito, draft.contradictedCodes(), catalogQueryPort.findItemIdsByCode());
        ProposalPresentation presentacion = ProposalAssembler.presentacion(resultado.outcome(),
                draft);
        metrics.proposalServed(ServedProposal.de(Operation.REFINE, resultado.outcome(),
                presentacion, draft, carrito, texto.length(), guardada.getId()));

        // Aqui vivia el suelo de latencia aleatorio de la ruta degradada. Se retiro:
        // el bit que ocultaba lo publica la respuesta. Ver
        // ProposalAssembler.presentacion.
        return reader.vista(guardada, carrito, catalog, presentacion, true);
    }

    /**
     * La regla de fusion de S8.3, y el motivo de cada linea:
     *
     * <ul>
     * <li>lo que el modelo propone entra;</li>
     * <li><strong>lo que el cliente quito NO vuelve</strong>, aunque el modelo
     * insista: una accion explicita pesa mas que una inferencia, y re-anadirla es
     * como alguien contrata en un movil algo que ya habia rechazado;</li>
     * <li><strong>lo que el cliente anadio se conserva</strong> aunque el modelo lo
     * omita.</li>
     * </ul>
     *
     * <p>
     * Sin esta fusion la degradacion es especialmente cruel: un turno que falla
     * -sin lineas- vaciaria el carrito entero del prospecto.
     */
    private List<String> fusionar(ProposalDraft draft, List<ProposalTurn> turnos,
            CartResult vigente) {
        Set<String> retiradas = reader.retiradasPorElCliente(turnos);
        Set<String> anadidas = reader.anadidasPorElCliente(turnos);
        Set<String> fusionado = new LinkedHashSet<>(
                draft.tieneLineas() ? draft.necessaryCodes() : reader.codigosAceptados(vigente));
        fusionado.addAll(anadidas);
        fusionado.removeAll(retiradas);
        return List.copyOf(fusionado);
    }
}
