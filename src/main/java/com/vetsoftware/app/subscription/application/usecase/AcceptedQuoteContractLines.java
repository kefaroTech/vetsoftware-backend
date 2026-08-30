package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.command.SubscriptionItemLineCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemSnapshot;
import com.vetsoftware.app.subscription.application.dto.SubscriptionQuoteSnapshot;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * <strong>Lo que una cotizacion aceptada tiene que cumplir antes de convertirse
 * en contrato, y como se copia.</strong>
 *
 * <p>
 * Vive aqui y no dentro de un servicio por la misma razon que
 * {@code SelfServeCartGuard} en la rodaja {@code quote}: hay <em>dos</em>
 * caminos que firman un contrato a partir de una oferta aceptada —el alta
 * pedida por la consola ({@code CreateRequestedSubscriptionService}) y la
 * sustitucion que dispara la propia aceptacion
 * ({@code ReplaceSubscriptionFromQuoteService})—, y <strong>los dos tienen que
 * aceptar exactamente la misma oferta y copiarla exactamente igual</strong>. Si
 * divergieran, el contrato firmado por un camino diria algo distinto del
 * firmado por el otro a partir del mismo papel, que es la peor forma posible de
 * este defecto: los dos parecen correctos por separado.
 *
 * <p>
 * <strong>Aqui no se reparte ni se recalcula nada, se COPIA.</strong> Los
 * renglones ya vienen partidos por tramo desde que se emitio la oferta (D-66) y
 * cada uno trae su tramo, su precio, su descuento y su marca de condicionado
 * congelados (D-86). Cualquier cuenta que se rehiciera aqui podria dar otro
 * numero, y un contrato que no diga exactamente lo que decia el papel que firmo
 * el cliente no vale nada.
 */
final class AcceptedQuoteContractLines {

    /**
     * El actor de la bitacora cuando la oferta no dejo correo de quien acepto. No
     * deberia ocurrir —{@code Quote.accept} exige el correo— pero la bitacora es
     * probatoria y prefiere un actor generico a uno vacio.
     */
    private static final String SYSTEM_ACTOR = "SYSTEM";

    private AcceptedQuoteContractLines() {
    }

    /**
     * Valida la oferta y devuelve sus renglones ya traducidos a lineas de contrato.
     *
     * @param expectedPriceListId
     *            tarifa que el llamante cree estar firmando. Se comprueba contra la
     *            de la oferta: firmar con una tarifa distinta de la cotizada es
     *            firmar otra cosa
     * @param expectedCycle
     *            ciclo que el llamante cree estar firmando, por lo mismo. Cada
     *            ciclo lleva su propio precio en la tarifa, asi que no es un
     *            detalle de presentacion
     */
    static ResolvedContractLines from(SubscriptionQuoteSnapshot quote, Long expectedPriceListId,
            BillingCycle expectedCycle) {
        if (quote == null)
            throw new IllegalArgumentException("an accepted quote snapshot is required");
        if (!quote.accepted())
            throw new IllegalStateException("Quote must be ACCEPTED: " + quote.id());
        if (!Objects.equals(quote.priceListId(), expectedPriceListId))
            throw new IllegalArgumentException("priceListId does not match accepted quote");
        if (quote.billingCycle() != expectedCycle)
            throw new IllegalArgumentException("billingCycle does not match accepted quote");
        if (quote.items() == null || quote.items().isEmpty())
            throw new IllegalStateException("Accepted quote has no contract lines: " + quote.id());
        return new ResolvedContractLines(actorOf(quote),
                quote.items().stream().map(item -> toLine(item, null, null)).toList());
    }

    /**
     * Quien firma. Es el correo que dejo la aceptacion, que es la unica prueba de
     * quien acepto; sin el, {@code SYSTEM}.
     */
    private static String actorOf(SubscriptionQuoteSnapshot quote) {
        return quote.acceptedBy() == null || quote.acceptedBy().isBlank()
                ? SYSTEM_ACTOR
                : quote.acceptedBy();
    }

    /** La copia, campo a campo. Ni una sola operacion aritmetica. */
    static SubscriptionItemLineCommand toLine(SubscriptionItemSnapshot snapshot,
            LocalDate effectiveFrom, LocalDate effectiveTo) {
        return new SubscriptionItemLineCommand(snapshot.catalogItemId(), snapshot.itemCode(),
                snapshot.itemName(), snapshot.itemType(), snapshot.capacityUnit(),
                snapshot.tierMin(), snapshot.tierMax(), snapshot.includedQuantity(),
                snapshot.taxTreatment(), snapshot.quantity(), snapshot.unitAmount(),
                snapshot.discountPercent(), snapshot.discountAmount(),
                snapshot.discountIsConditional(), snapshot.taxRate(), effectiveFrom, effectiveTo);
    }

    static List<SubscriptionItemLineCommand> emptyIfNull(List<SubscriptionItemLineCommand> items) {
        return items == null ? List.of() : items;
    }
}
