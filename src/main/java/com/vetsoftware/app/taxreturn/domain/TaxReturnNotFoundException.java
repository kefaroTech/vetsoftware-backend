package com.vetsoftware.app.taxreturn.domain;

public class TaxReturnNotFoundException extends RuntimeException {

    public TaxReturnNotFoundException(Long id) {
        super("Tax return not found: " + id);
    }
}
