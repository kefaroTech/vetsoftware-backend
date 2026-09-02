package com.vetsoftware.app.aiproposal.application.usecase;

import com.vetsoftware.app.aiproposal.domain.CartLine;
import com.vetsoftware.app.aiproposal.domain.CartResult;
import com.vetsoftware.app.aiproposal.domain.GenerationOutcome;
import com.vetsoftware.app.aiproposal.domain.LineAction;
import com.vetsoftware.app.aiproposal.domain.PackComparison;
import com.vetsoftware.app.aiproposal.domain.PackComparisonResult;
import com.vetsoftware.app.aiproposal.domain.ProposalDraft;
import com.vetsoftware.app.aiproposal.domain.ProposalLine;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import com.vetsoftware.app.aiproposal.domain.SellableItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Las tres traducciones que los cuatro casos de uso comparten, sin estado y sin
 * Spring.
 *
 * <p>
 * Vive en {@code application} y no en {@code domain} porque trabaja con
 * {@link ProposalLine} -que es persistencia del turno- y con
 * {@link GenerationOutcome} -que es resultado de un puerto de salida-, y
 * ninguna de las dos cosas es una regla de negocio.
 */
final class ProposalAssembler {

    private ProposalAssembler() {
    }

    /**
     * Rehace el carrito a partir de las lineas ya escritas.
     *
     * <p>
     * <strong>El importe sale de la linea y el resto del catalogo</strong>, y esa
     * division es deliberada: {@code unit_amount} se congelo al cotizar -es un
     * registro de auditoria, no una foto que cambia sola cuando se publica una
     * tarifa nueva- mientras que el nombre, la descripcion, el impuesto y los dias
     * de prueba se leen del catalogo vigente, porque son <em>copy</em> y no dinero
     * comprometido.
     *
     * <p>
     * Una linea rechazada cuyo codigo el catalogo no conoce -la alucinacion del
     * modelo, que es el dato que mide su calidad- se reconstruye igual: no lleva
     * precio ni divisa y por eso {@code CartLine} la admite sin ellos.
     *
     * <p>
     * &#9940; <strong>Las lineas {@link LineAction#REMOVED} no son
     * carrito.</strong> {@code escribirEdicion} las escribe en el mismo turno que
     * el carrito para dejar constancia de que el cliente veto ese codigo, y son
     * {@code verdict = ACCEPTED} con {@code unit_amount} a {@code NULL} -no hay
     * nada que cobrar por una linea que se quito-. Incluirlas aqui hacia dos danos
     * a la vez: {@code CartLine} rechaza una linea aceptada sin precio, de modo que
     * <em>cualquier</em> relectura posterior a una edicion que quitara algo -el GET
     * del enlace del correo, el refinamiento siguiente, la edicion siguiente- moria
     * con un 500; y si no muriera, el codigo vetado volveria al carrito por
     * {@code codigosAceptados}, que es justo lo contrario de lo que el cliente
     * pidio.
     */
    static CartResult reconstruir(List<ProposalLine> lineas, SellableCatalog catalog) {
        List<CartLine> reconstruidas = new ArrayList<>();
        for (ProposalLine linea : lineas.stream()
                .filter(escrita -> escrita.getAction() != LineAction.REMOVED)
                .sorted(Comparator.comparingInt(ProposalLine::getSortOrder)).toList()) {
            SellableItem item = catalog.find(linea.getItemCode()).orElse(null);
            reconstruidas.add(new CartLine(linea.getItemCode(),
                    item == null ? linea.getItemCode() : item.name(),
                    item == null ? null : item.shortDescription(),
                    item == null ? null : item.kind(), linea.getSource(), linea.getVerdict(),
                    linea.getQuantity(), linea.getUnitAmount(),
                    item == null ? null : item.taxRate(), item == null ? 0 : item.trialDays(),
                    item == null ? null : item.currency(), linea.getReason(),
                    linea.getSortOrder()));
        }
        return new CartResult(reconstruidas, divisa(reconstruidas, catalog));
    }

    /** El carrito vacio de {@code OUT_OF_DOMAIN}: ni una linea, ni de ejemplo. */
    static CartResult vacio(SellableCatalog catalog) {
        return new CartResult(List.of(), divisa(List.of(), catalog));
    }

    static Optional<PackComparisonResult> oferta(CartResult carrito, SellableCatalog catalog) {
        return PackComparison.mejorOferta(carrito, catalog);
    }

    /**
     * &#9940; Las cuatro poblaciones que la telemetria separa hacia dentro colapsan
     * aqui en tres estados de pantalla, y las <strong>tres</strong> degradaciones
     * mas el fallo del modelo dan el mismo: un anonimo no puede distinguir "se
     * agoto el presupuesto" de "la palanca esta apagada" de "el modelo fallo".
     *
     * <p>
     * &#9940; <strong>LAPIDA — no reintroduzcas el suelo de latencia de la ruta
     * degradada.</strong> Existio: {@code RandomizedResponsePacing} dormia el hilo
     * del servlet entre 2.500 y 4.500 ms en cada respuesta degradada para que nadie
     * distinguiera con un cronometro "hubo generacion real" (3-8 s) de "se agoto el
     * presupuesto" (milisegundos). El argumento se lee perfecto y es falso desde el
     * dia en que esta linea existe: <strong>el bit que ese suelo ocultaba lo
     * publica la respuesta en texto plano</strong>. Todo lo que no sea
     * {@code SUCCEEDED} sale por aqui como {@code DETERMINISTIC}, el controller lo
     * serializa en {@code presentation} y {@code VetSoftwarePublicFront} lo usa
     * como discriminador para elegir pantalla. Una sola peticion, sin cronometro y
     * sin estadistica, lee el estado de degradacion de la plataforma.
     *
     * <p>
     * Asi que el suelo costaba un hilo de servidor bloqueado hasta 4,5 s por
     * peticion degradada -justo cuando la plataforma ya esta en apuros- a cambio de
     * cerrar un canal que la linea de al lado deja abierto. Se retiro el suelo y
     * <strong>no</strong> el campo: quitar {@code presentation} seria un cambio de
     * contrato que rompe el front, y el bit ya es publico por diseno.
     *
     * <p>
     * <strong>Si algun dia hace falta ocultarlo de verdad</strong>, el orden es al
     * reves: primero dejar de publicar el estado -que {@code DETERMINISTIC} y
     * {@code PROPOSAL} sean indistinguibles para el cliente, que hoy ademas
     * renderizan la misma pantalla- y solo entonces igualar la latencia. Poner el
     * suelo primero es pagar por cerrar una puerta abierta.
     */
    static ProposalPresentation presentacion(GenerationOutcome outcome, ProposalDraft draft) {
        if (outcome != GenerationOutcome.SUCCEEDED)
            return ProposalPresentation.DETERMINISTIC;
        if (draft.outOfDomain())
            return ProposalPresentation.OUT_OF_DOMAIN;
        if (!draft.understood())
            return ProposalPresentation.NOT_UNDERSTOOD;
        return ProposalPresentation.PROPOSAL;
    }

    /**
     * La divisa de un carrito reconstruido: la de sus lineas aceptadas y, si no hay
     * ninguna, la del nucleo. <strong>Sin {@code orElseThrow}</strong>: aquel
     * cubria el catalogo sin nucleo, que desde el rediseno no es construible
     * ({@link SellableCatalog}).
     */
    private static String divisa(List<CartLine> lineas, SellableCatalog catalog) {
        return lineas.stream().filter(linea -> linea.verdict().esAceptado()).map(CartLine::currency)
                .filter(java.util.Objects::nonNull).findFirst().orElseGet(catalog::currency);
    }
}
