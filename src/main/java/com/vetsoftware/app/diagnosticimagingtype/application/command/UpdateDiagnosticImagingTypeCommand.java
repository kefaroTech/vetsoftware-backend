package com.vetsoftware.app.diagnosticimagingtype.application.command;

public record UpdateDiagnosticImagingTypeCommand(Long id, String name, String description,
        Long companyId, boolean general) {
}
