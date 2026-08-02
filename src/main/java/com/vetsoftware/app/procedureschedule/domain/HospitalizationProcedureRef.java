package com.vetsoftware.app.procedureschedule.domain;

public record HospitalizationProcedureRef(Long id, String name) {
    public HospitalizationProcedureRef {
        if (id == null)
            throw new IllegalArgumentException("hospitalization procedure id is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("hospitalization procedure name is required");
    }
}
