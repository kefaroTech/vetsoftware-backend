package com.vetsoftware.app.pricelist.domain;

/**
 * Transición de estado no permitida. Las únicas legales son
 * {@code DRAFT → PUBLISHED} y {@code PUBLISHED → ARCHIVED}.
 */
public class InvalidPriceListTransitionException extends RuntimeException {

    private final PriceListStatus from;
    private final PriceListStatus to;

    public InvalidPriceListTransitionException(PriceListStatus from, PriceListStatus to) {
        super("Invalid price list transition: " + from + " -> " + to);
        this.from = from;
        this.to = to;
    }

    public PriceListStatus getFrom() {
        return from;
    }

    public PriceListStatus getTo() {
        return to;
    }
}
