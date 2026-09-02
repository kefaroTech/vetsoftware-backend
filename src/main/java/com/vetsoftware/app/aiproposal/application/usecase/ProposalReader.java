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
import java.time.Clock;
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

    /**
     * Turnos de modelo por propuesta: el inicial mas tres refinamientos.
     *
     * <p>
     * Es <b>publico</b> porque hay otro sitio que tiene que conocer el numero:
     * {@code LoginRateLimitFilter} reparte el presupuesto diario en sesiones
     * completas, y una sesion son exactamente estas cuatro llamadas de pago. No
     * puede importarlo -es otra rodaja- asi que declara su propia constante y el
     * test ata las dos.
     */
    public static final int MAX_TURNOS_DE_MODELO = 4;

    private final AiProposalRepository repository;

    private final SellableCatalogQueryPort catalogQueryPort;

    private final Clock clock;

    public ProposalReader(AiProposalRepository repository,
            SellableCatalogQueryPort catalogQueryPort, Clock clock) {
        this.repository = repository;
        this.catalogQueryPort = catalogQueryPort;
        this.clock = clock;
    }

    /**
     * La propuesta que el token senala, <strong>si todavia esta vigente</strong>.
     *
     * <p>
     * &#9940; <strong>La caducidad se comprueba aqui y no en cada caso de
     * uso.</strong> Este es el unico punto por el que las tres rutas publicas
     * -{@code GET}, {@code refine} y el {@code PUT} de lineas- alcanzan una
     * propuesta: {@code GetProposalService}, {@code RefineProposalService} y
     * {@code EditProposalLinesService} empiezan los tres llamando a este metodo.
     * Comprobarlo en cada uno seria tres sitios donde olvidarlo, y el cuarto
     * consumidor que llegue nacería sin la comprobacion.
     *
     * <p>
     * <strong>Sin esto el token era una credencial al portador permanente.</strong>
     * {@code expiresAt} se escribia al crear la propuesta, viajaba en la respuesta,
     * el correo renderiza "caduca el DD/MM/YYYY" a partir de el y <em>nadie lo leia
     * nunca</em>: un enlace filtrado -reenviado, indexado, en un historial de
     * navegador compartido- seguia sirviendo la propuesta entera y dejando editarla
     * anos despues.
     *
     * <p>
     * <strong>Y es 404, no 410.</strong> Un 410 seria mas informativo y ese es
     * justo el problema: distinguiria "este token nunca existio" de "este token
     * existio y caduco" para quien esta probando tokens a ciegas, que es un oraculo
     * de existencia sobre la unica frontera de autorizacion de la feature. Quien
     * tiene un enlace legitimo no necesita el 410 para enterarse: mientras la
     * propuesta vive, la respuesta le lleva {@code expiresAt} dentro y el correo se
     * lo dijo por escrito. El 404 es ademas lo que ya afirmaban por escrito este
     * javadoc y el de {@code GlobalExceptionHandler}; lo que faltaba era el codigo
     * que lo cumpliera.
     */
    public AiProposal exigir(String publicToken) {
        AiProposal proposal = repository.findByPublicToken(publicToken)
                .orElseThrow(AiProposalNotFoundException::new);
        if (proposal.haCaducado(clock))
            throw new AiProposalNotFoundException();
        return proposal;
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

    /**
     * La vista de una propuesta ya persistida, que es lo que sirve el GET.
     *
     * <p>
     * &#9940; <strong>La pantalla y las lineas se buscan por separado, y esa
     * separacion es un arreglo.</strong> Antes {@code vigente} se asignaba
     * <em>dentro</em> del {@code if (!delTurno.isEmpty())}, asi que un turno con
     * {@code presentation} escrita en base <strong>y sin ni una linea</strong>
     * quedaba inalcanzable: {@link #presentacionReleida} no lo veia nunca y caia al
     * respaldo derivado, que con el carrito vacio devuelve
     * {@link ProposalPresentation#OUT_OF_DOMAIN}. El dano no era cosmetico: al
     * prospecto que escribio «tengo una veterinaria» y recibio
     * {@code NOT_UNDERSTOOD} —una invitacion a reformular— se le decia, al abrir el
     * enlace del correo, que <em>su negocio no es de los nuestros</em>; y en el
     * paso vinculante, que su propuesta ya no existe.
     *
     * <p>
     * <strong>La pantalla la impone el turno del carrito; solo si NINGUN turno
     * escribio lineas manda la ultima pantalla persistida.</strong> Ese matiz es
     * todo el arreglo, y hay que leerlo junto a
     * {@code un_turno_sin_lineas_no_cambia_la_pantalla}: un turno posterior que
     * fallo sin escribir nada <em>no</em> puede repintar el carrito de otro turno,
     * porque lo que el prospecto tiene delante es aquel carrito y aquella pantalla.
     * Lo que si tiene que poder es hablar cuando no hay carrito ninguno —que es
     * justo el caso que estaba roto—.
     *
     * <p>
     * Las cuatro poblaciones, y solo la ultima cambia:
     * <ul>
     * <li>un turno con lineas: su pantalla, como siempre;</li>
     * <li>un {@code CUSTOMER_EDIT}, que no anota pantalla a proposito: se deriva
     * del carrito, como siempre;</li>
     * <li>un turno sin lineas <em>por encima</em> de otro que si las tiene: se
     * ignora, como siempre;</li>
     * <li><strong>ningun turno con lineas</strong>: antes esto caia al respaldo
     * derivado —{@link ProposalPresentation#OUT_OF_DOMAIN} con el carrito vacio— y
     * ahora sirve la ultima pantalla escrita.</li>
     * </ul>
     * No cuesta ni una consulta mas: el bucle sigue parando en el primer turno con
     * lineas.
     */
    public ProposalViewDto vista(AiProposal proposal, boolean recalculated) {
        SellableCatalog catalog = catalogo(proposal);
        List<ProposalTurn> turnos = repository.findTurnsByProposalId(proposal.getId());
        ProposalTurn conLineas = null;
        ProposalTurn ultimoConPantalla = null;
        List<ProposalLine> lineas = List.of();
        for (int i = turnos.size() - 1; i >= 0; i--) {
            ProposalTurn turno = turnos.get(i);
            if (ultimoConPantalla == null && turno.getPresentation() != null)
                ultimoConPantalla = turno;
            List<ProposalLine> delTurno = repository.findLinesByTurnId(turno.getId());
            if (!delTurno.isEmpty()) {
                conLineas = turno;
                lineas = delTurno;
                break;
            }
        }
        ProposalTurn vigente = conLineas != null ? conLineas : ultimoConPantalla;
        CartResult carrito = ProposalAssembler.reconstruir(lineas, catalog);
        return vista(proposal, carrito, catalog, presentacionReleida(vigente, carrito),
                recalculated);
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
     * &#9940; <strong>La pantalla que se sirvio, leida de donde se
     * escribio.</strong> El turno de modelo la persiste en
     * {@code ai_proposal_turns.presentation} (changeset 388), asi que una relectura
     * devuelve exactamente lo que el prospecto vio, incluidas
     * {@link ProposalPresentation#NOT_UNDERSTOOD} y
     * {@link ProposalPresentation#DETERMINISTIC}.
     *
     * <p>
     * <strong>La derivacion sigue aqui, y solo como respaldo</strong>, para dos
     * poblaciones legitimas: las filas escritas antes del changeset 388 y los
     * turnos {@code CUSTOMER_EDIT}, que no anotan pantalla a proposito -despues de
     * una edicion manual la pantalla es la propuesta, no el desenlace del modelo
     * que la precedio-. Ese respaldo es exactamente lo que habia antes y arrastra
     * su limitacion: separa {@link ProposalPresentation#OUT_OF_DOMAIN} -el unico
     * camino que no deja ni una linea aceptada- y funde el resto en
     * {@link ProposalPresentation#PROPOSAL}.
     */
    private static ProposalPresentation presentacionReleida(ProposalTurn vigente,
            CartResult carrito) {
        if (vigente != null && vigente.getPresentation() != null)
            return vigente.getPresentation();
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
