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

    private static String divisa(List<CartLine> lineas, SellableCatalog catalog) {
        return lineas.stream().filter(linea -> linea.verdict().esAceptado()).map(CartLine::currency)
                .filter(java.util.Objects::nonNull).findFirst().or(catalog::currency)
                .orElseThrow(() -> new IllegalStateException(
                        "an empty catalog cannot price a proposal"));
    }
}
