package com.vetsoftware.app.diagnosticimagingtype.application.command;

public record CreateDiagnosticImagingTypeCommand(String name, String description, Long companyId,
        boolean general) {
}
