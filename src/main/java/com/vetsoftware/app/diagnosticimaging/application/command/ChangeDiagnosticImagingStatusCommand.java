package com.vetsoftware.app.diagnosticimaging.application.command;

public record ChangeDiagnosticImagingStatusCommand(Long id, String status, Long companyId) {}
