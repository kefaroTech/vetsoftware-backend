package com.vetsoftware.app.pricelist.domain;

/**
 * Se intentó modificar una lista de precios que ya no está en {@code DRAFT}, o
 * uno de sus precios.
 *
 * <p>
 * Es la regla R9 de {@code suscripciones-reglas-codigo.md}: <em>una lista de
 * precios publicada es inmutable, ella y sus precios</em>. MySQL no la puede
 * imponer —un {@code CHECK} se evalúa sobre la fila resultante y no ve el valor
 * anterior, así que puede comprobar «el estado actual es válido» pero no «este
 * cambio estaba permitido»—, de modo que el único guardián es este. Si se
 * rompe, cambia retroactivamente lo que se le ofreció a un cliente que ya
 * firmó: la vía directa a una reclamación que se pierde.
 */
public class PriceListNotEditableException extends RuntimeException {

    private final Long priceListId;
    private final PriceListStatus status;

    public PriceListNotEditableException(Long priceListId, PriceListStatus status) {
        super("Price list " + priceListId + " is " + status
                + " and cannot be modified: publish a new price list instead");
        this.priceListId = priceListId;
        this.status = status;
    }

    public Long getPriceListId() {
        return priceListId;
    }

    public PriceListStatus getStatus() {
        return status;
    }
}
