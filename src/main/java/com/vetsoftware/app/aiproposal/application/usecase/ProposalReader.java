package com.vetsoftware.app.aiproposal.application.usecase;

import com.vetsoftware.app.aiproposal.application.dto.ProposalViewDto;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalRepository;
import com.vetsoftware.app.aiproposal.application.port.out.SellableCatalogQueryPort;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.AiProposalNotFoundException;
import com.vetsoftware.app.aiproposal.domain.CartLine;
import com.vetsoftware.app.aiproposal.domain.CartResult;
import com.vetsoftware.app.aiproposal.domain.LineAction;
import com.vetsoftware.app.aiproposal.domain.LineSource;
import com.vetsoftware.app.aiproposal.domain.ProposalLine;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import com.vetsoftware.app.aiproposal.domain.ProposalTurn;
import com.vetsoftware.app.aiproposal.domain.ProposalVersionConflictException;
import com.vetsoftware.app.aiproposal.domain.ProspectText;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Lo que los cuatro casos de uso necesitan leer de una propuesta ya escrita: su
 * carrito vigente, su historial de texto y lo que el cliente decidio a mano.
 *
 * <p>
 * <strong>Sin {@code @Transactional} a proposito.</strong> Tres de sus cuatro
 * consumidores invocan al modelo despues de llamar aqui, y una anotacion en
 * esta clase pondria la lectura y la invocacion bajo la misma transaccion en
 * cuanto alguien las juntara. Son lecturas cortas por clave primaria o por
 * indice unico; no necesitan una.
 */
@Component
public class ProposalReader {

    /** Turnos de modelo por propuesta: el inicial mas tres refinamientos. */
    static final int MAX_TURNOS_DE_MODELO = 4;

    private final AiProposalRepository repository;

    private final SellableCatalogQueryPort catalogQueryPort;

    public ProposalReader(AiProposalRepository repository,
            SellableCatalogQueryPort catalogQueryPort) {
        this.repository = repository;
        this.catalogQueryPort = catalogQueryPort;
    }

    /**
     * La propuesta que el token senala.
     *
     * <p>
     * <strong>Un token que no existe y uno que existe y caduco dan el mismo
     * 404.</strong> No hay nada que ganar distinguiendolos y si algo que perder:
     * seria un oraculo de existencia sobre un identificador que alguien puede estar
     * probando.
     */
    public AiProposal exigir(String publicToken) {
        return repository.findByPublicToken(publicToken)
                .orElseThrow(AiProposalNotFoundException::new);
    }

    /**
     * El bloqueo optimista de las dos escrituras publicas. Una peticion sin version
     * declarada no se rechaza -el cliente viejo sigue funcionando- pero tampoco se
     * protege; una con version distinta es un 409 y el front recarga.
     */
    public void exigirVersion(AiProposal proposal, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(proposal.getVersion()))
            throw new ProposalVersionConflictException();
    }

    public SellableCatalog catalogo(AiProposal proposal) {
        return catalogQueryPort.loadCatalog(proposal.getPriceListId(), proposal.getBillingCycle())
                .orElseThrow(() -> new IllegalStateException(
                        "the price list that quoted this proposal is no longer readable"));
    }

    /**
     * Las lineas del ultimo turno que escribio alguna. Un turno de modelo que fallo
     * del todo -sin ni una linea- no borra el carrito anterior, que es lo que el
     * prospecto sigue viendo en pantalla.
     */
    public List<ProposalLine> lineasVigentes(AiProposal proposal) {
        List<ProposalTurn> turnos = repository.findTurnsByProposalId(proposal.getId());
        for (int i = turnos.size() - 1; i >= 0; i--) {
            List<ProposalLine> lineas = repository.findLinesByTurnId(turnos.get(i).getId());
            if (!lineas.isEmpty())
                return lineas;
        }
        return List.of();
    }

    public List<ProposalTurn> turnos(AiProposal proposal) {
        return repository.findTurnsByProposalId(proposal.getId());
    }

    public int siguienteNumeroDeTurno(List<ProposalTurn> turnos) {
        return turnos.stream().mapToInt(ProposalTurn::getTurnNumber).max().orElse(0) + 1;
    }

    public long turnosDeModelo(List<ProposalTurn> turnos) {
        return turnos.stream().filter(turno -> turno.getTurnType().invocaAlModelo()).count();
    }

    /**
     * &#9940; <strong>Los textos del cliente, acumulados y en orden.</strong>
     * Mandar solo el ultimo es la regresion silenciosa de S7.2.1: el prospecto
     * escribe cuatro frases, recibe ocho lineas, anade "tambien hacemos peluqueria"
     * y -si ese fuera todo el contexto- el modelo devolveria {@code GROOMING} y
     * nada mas, con razon. La regla de fusion borraria entonces las siete lineas
     * del primer turno una a una, justo debajo de un copy que promete "no hace
     * falta que repitas lo de antes".
     */
    public List<ProspectText> textosDelCliente(List<ProposalTurn> turnos) {
        return turnos.stream().filter(turno -> turno.getTurnType().invocaAlModelo())
                .map(ProposalTurn::getInputText).filter(texto -> texto != null && !texto.isBlank())
                .map(ProspectText::of).toList();
    }

    /**
     * Lo que el cliente quito a mano, en cualquier turno.
     *
     * <p>
     * <strong>Es la mitad que hace soberana la edicion manual.</strong> El usuario
     * quita "Facturacion electronica" porque no factura, escribe "tambien hacemos
     * peluqueria", y sin esto la facturacion vuelve: con ocho lineas en un movil no
     * se fija y contrata algo que rechazo.
     */
    public Set<String> retiradasPorElCliente(List<ProposalTurn> turnos) {
        Set<String> retiradas = new LinkedHashSet<>();
        for (ProposalTurn turno : turnos) {
            for (ProposalLine linea : repository.findLinesByTurnId(turno.getId())) {
                if (linea.getAction() == LineAction.REMOVED
                        && linea.getSource() == LineSource.CUSTOMER)
                    retiradas.add(linea.getItemCode());
            }
        }
        return retiradas;
    }

    /**
     * Lo que el cliente anadio a mano y hay que conservar aunque el modelo calle.
     */
    public Set<String> anadidasPorElCliente(List<ProposalTurn> turnos) {
        Set<String> anadidas = new LinkedHashSet<>();
        for (ProposalTurn turno : turnos) {
            for (ProposalLine linea : repository.findLinesByTurnId(turno.getId())) {
                if (linea.getAction() == LineAction.ADDED
                        && linea.getSource() == LineSource.CUSTOMER
                        && linea.getVerdict().esAceptado())
                    anadidas.add(linea.getItemCode());
            }
        }
        return anadidas;
    }

    public List<String> codigosAceptados(CartResult carrito) {
        List<String> codigos = new ArrayList<>();
        for (CartLine linea : carrito.aceptadas())
            codigos.add(linea.code());
        return codigos;
    }

    /** La vista de una propuesta ya persistida, que es lo que sirve el GET. */
    public ProposalViewDto vista(AiProposal proposal, boolean recalculated) {
        SellableCatalog catalog = catalogo(proposal);
        CartResult carrito = ProposalAssembler.reconstruir(lineasVigentes(proposal), catalog);
        return vista(proposal, carrito, catalog, presentacionReleida(carrito), recalculated);
    }

    public ProposalViewDto vista(AiProposal proposal, CartResult carrito, SellableCatalog catalog,
            ProposalPresentation presentacion, boolean recalculated) {
        return ProposalViewDto.de(proposal.getPublicToken(), presentacion, proposal.getExpiresAt(),
                proposal.getVersion(), carrito, ProposalAssembler.oferta(carrito, catalog),
                refinamientosRestantes(proposal), recalculated);
    }

    public int refinamientosRestantes(AiProposal proposal) {
        long usados = Math.max(0, turnosDeModelo(turnos(proposal)) - 1);
        return (int) Math.max(0, MAX_TURNOS_DE_MODELO - 1 - usados);
    }

    /**
     * &#9888; <strong>Lo que una relectura no puede reconstruir.</strong> El estado
     * de pantalla se calcula al responder y <em>no se persiste</em>: no hay columna
     * para el. Aqui se deriva del unico rastro que queda -si el turno vigente dejo
     * alguna linea aceptada-, y eso separa bien
     * {@link ProposalPresentation#OUT_OF_DOMAIN} -el unico camino que no escribe ni
     * una linea aceptada, porque a un negocio ajeno no se le ofrece ni un punto de
     * partida- de todo lo demas, pero <strong>funde
     * {@link ProposalPresentation#NOT_UNDERSTOOD} y la degradacion en
     * {@link ProposalPresentation#PROPOSAL}</strong>: las dos escriben el carrito
     * determinista, que siempre lleva el nucleo. Distinguirlas al releer exige una
     * columna, y esa columna no existe en el changeset 384.
     */
    private static ProposalPresentation presentacionReleida(CartResult carrito) {
        return carrito.aceptadas().isEmpty()
                ? ProposalPresentation.OUT_OF_DOMAIN
                : ProposalPresentation.PROPOSAL;
    }

    public Optional<AiProposal> porIdempotencia(String contactEmail, String idempotencyKey) {
        if (idempotencyKey == null)
            return Optional.empty();
        return repository.findByIdempotency(contactEmail, idempotencyKey);
    }
}
