package com.vetsoftware.app.productchargeopenaccount.domain;

public class ProductChargeOpenAccountNotFoundException extends RuntimeException {
    public ProductChargeOpenAccountNotFoundException(Long id) {
        super("ProductChargeOpenAccount not found: " + id);
    }
}
