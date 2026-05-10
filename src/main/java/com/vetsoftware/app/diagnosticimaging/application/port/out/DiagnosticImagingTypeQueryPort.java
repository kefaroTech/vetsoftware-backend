package com.vetsoftware.app.diagnosticimaging.application.port.out;

import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingTypeRef;
import java.util.Optional;

public interface DiagnosticImagingTypeQueryPort {
    Optional<DiagnosticImagingTypeRef> findById(Long diagnosticImagingTypeId);
}
