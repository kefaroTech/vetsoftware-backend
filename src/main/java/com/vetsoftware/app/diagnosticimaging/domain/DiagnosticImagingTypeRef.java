package com.vetsoftware.app.diagnosticimaging.domain;

public record DiagnosticImagingTypeRef(Long id, String name) {
    public DiagnosticImagingTypeRef {
        if (id == null) throw new IllegalArgumentException("diagnostic imaging type id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("diagnostic imaging type name is required");
    }
}
