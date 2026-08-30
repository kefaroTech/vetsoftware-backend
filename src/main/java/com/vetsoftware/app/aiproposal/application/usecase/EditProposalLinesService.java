package com.vetsoftware.app.aiproposal.application.usecase;

import com.vetsoftware.app.aiproposal.application.command.EditProposalLinesCommand;
import com.vetsoftware.app.aiproposal.application.dto.ProposalViewDto;
import com.vetsoftware.app.aiproposal.application.port.in.EditProposalLinesUseCase;
import com.vetsoftware.app.aiproposal.application.port.out.SellableCatalogQueryPort;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.CartResult;
import com.vetsoftware.app.aiproposal.domain.LineSource;
import com.vetsoftware.app.aiproposal.domain.ProposalCart;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import com.vetsoftware.app.aiproposal.domain.ProposalTurn;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * La edicion manual. <strong>No llama al modelo</strong>, asi que no hay
 * ninguna transaccion que partir ni ningun suelo de latencia que aplicar: aqui
 * no hay presupuesto que filtrar por el reloj.
 *
 * <p>
 * &#9940; <strong>El carrito se reconstruye entero con el motor determinista, y
 * no se parchea a mano.</strong> Quitar "Caja" tiene que arrastrar lo que
 * dependia de ella, y anadir "Cuentas abiertas" tiene que traerse su
 * {@code REQUIRES} con el mismo cierre en anchura que aplica una propuesta del
 * modelo. Editar la lista a mano dejaria carritos imposibles que la
 * contratacion rechaza despues -en el paso 6, cuando el prospecto ya se
 * registro y verifico el correo- con un texto que ni siquiera le dice que linea
 * sobra.
 *
 * <p>
 * <strong>El turno se escribe con {@code source = CUSTOMER}</strong>: registra
 * el carrito tal como lo dejo el cliente. La autoria del modelo sigue viva,
 * intacta, en las lineas de su propio turno.
 */
@Service
public class EditProposalLinesService implements EditProposalLinesUseCase {

    private final SellableCatalogQueryPort catalogQueryPort;

    private final ProposalTurnWriter writer;

    private final ProposalReader reader;

    public EditProposalLinesService(SellableCatalogQueryPort catalogQueryPort,
            ProposalTurnWriter writer, ProposalReader reader) {
        this.catalogQueryPort = catalogQueryPort;
        this.writer = writer;
        this.reader = reader;
    }

    @Override
    public ProposalViewDto edit(EditProposalLinesCommand command) {
        AiProposal proposal = reader.exigir(command.publicToken());
        reader.exigirVersion(proposal, command.expectedVersion());

        SellableCatalog catalog = reader.catalogo(proposal);
        List<ProposalTurn> turnos = reader.turnos(proposal);
        CartResult vigente = ProposalAssembler.reconstruir(reader.lineasVigentes(proposal),
                catalog);

        Set<String> codigos = new LinkedHashSet<>(reader.codigosAceptados(vigente));
        codigos.removeAll(command.removedCodes());
        codigos.addAll(command.addedCodes());

        // Los motivos del turno anterior se conservan: una linea que sigue en el
        // carrito no pierde su explicacion porque el cliente haya tocado otra.
        CartResult carrito = ProposalCart.build(List.copyOf(codigos), List.of(),
                motivosVigentes(vigente), catalog, LineSource.CUSTOMER);

        Map<String, Long> ids = catalogQueryPort.findItemIdsByCode();
        AiProposal guardada = writer.escribirEdicion(proposal,
                reader.siguienteNumeroDeTurno(turnos), carrito,
                retiradasEfectivas(command, vigente), ids, null);

        return reader.vista(guardada, carrito, catalog, ProposalPresentation.PROPOSAL, false);
    }

    private static Map<String, String> motivosVigentes(CartResult vigente) {
        Map<String, String> motivos = new java.util.LinkedHashMap<>();
        vigente.lineas().stream().filter(linea -> linea.reason() != null)
                .forEach(linea -> motivos.putIfAbsent(linea.code(), linea.reason()));
        return motivos;
    }

    /**
     * Solo se registra como retirada la linea que <strong>estaba</strong> en el
     * carrito. Escribir un {@code REMOVED} de algo que nunca estuvo dejaria al
     * cliente vetando codigos que no habia visto, y el refinamiento siguiente los
     * respetaria: un cliente puede vaciar su propia propuesta futura mandando una
     * lista de codigos que nadie le ofrecio.
     */
    private static List<String> retiradasEfectivas(EditProposalLinesCommand command,
            CartResult vigente) {
        Set<String> enCarrito = new LinkedHashSet<>();
        vigente.aceptadas().forEach(linea -> enCarrito.add(linea.code()));
        List<String> retiradas = new ArrayList<>();
        for (String codigo : command.removedCodes()) {
            if (enCarrito.contains(codigo))
                retiradas.add(codigo);
        }
        return retiradas;
    }
}
