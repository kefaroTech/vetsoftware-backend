package com.vetsoftware.app.pricelist.domain;

public class PriceListNotFoundException extends RuntimeException {
    public PriceListNotFoundException(Long id) {
        super("Price list not found: " + id);
    }
}
