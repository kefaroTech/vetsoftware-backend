package com.vetsoftware.app.companyusageevent.domain;

/** El hecho de uso pedido no existe. Se traduce a HTTP 404. */
public class CompanyUsageEventNotFoundException extends RuntimeException {

    public CompanyUsageEventNotFoundException(Long id) {
        super("Company usage event not found: " + id);
    }

    public CompanyUsageEventNotFoundException(String message) {
        super(message);
    }
}
