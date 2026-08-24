package com.vetsoftware.app.quote.domain;

public class QuoteNotFoundException extends RuntimeException {
    public QuoteNotFoundException(Long id) {
        super("Quote not found: " + id);
    }
}
