package com.vetsoftware.app.diagnosticimaging.application.dto;

import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingTypeRef;

public record DiagnosticImagingTypeSummaryDto(Long id, String name) {
    public static DiagnosticImagingTypeSummaryDto from(DiagnosticImagingTypeRef ref) {
        return new DiagnosticImagingTypeSummaryDto(ref.id(), ref.name());
    }
}
