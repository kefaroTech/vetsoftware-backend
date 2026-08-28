package com.vetsoftware.app.supplierwithholding.domain;

public class SupplierWithholdingNotFoundException extends RuntimeException {

    public SupplierWithholdingNotFoundException(Long id) {
        super("Supplier withholding not found: " + id);
    }
}
