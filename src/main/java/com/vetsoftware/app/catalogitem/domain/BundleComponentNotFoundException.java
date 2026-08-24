package com.vetsoftware.app.catalogitem.domain;

public class BundleComponentNotFoundException extends RuntimeException {
    public BundleComponentNotFoundException(Long id) {
        super("BundleComponent not found: " + id);
    }
}
