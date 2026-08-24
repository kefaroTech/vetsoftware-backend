package com.vetsoftware.app.entitlement.domain;

/** El permiso derivado que se pidio no existe para esa empresa. */
public class CompanyEntitlementNotFoundException extends RuntimeException {

    public CompanyEntitlementNotFoundException(String message) {
        super(message);
    }
}
