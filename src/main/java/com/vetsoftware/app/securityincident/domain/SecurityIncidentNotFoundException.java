package com.vetsoftware.app.securityincident.domain;

public class SecurityIncidentNotFoundException extends RuntimeException {

    public SecurityIncidentNotFoundException(Long id) {
        super("Security incident not found: " + id);
    }
}
