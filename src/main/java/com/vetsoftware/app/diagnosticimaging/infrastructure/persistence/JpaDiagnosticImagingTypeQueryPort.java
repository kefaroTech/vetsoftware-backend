package com.vetsoftware.app.diagnosticimaging.infrastructure.persistence;

import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingTypeQueryPort;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingTypeRef;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence.DiagnosticImagingTypeJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("diagnosticImagingJpaDiagnosticImagingTypeQueryPort")
public class JpaDiagnosticImagingTypeQueryPort implements DiagnosticImagingTypeQueryPort {
  private final DiagnosticImagingTypeJpaRepository diagnosticImagingTypeJpaRepository;

  public JpaDiagnosticImagingTypeQueryPort(
      DiagnosticImagingTypeJpaRepository diagnosticImagingTypeJpaRepository) {
    this.diagnosticImagingTypeJpaRepository = diagnosticImagingTypeJpaRepository;
  }

  @Override
  public Optional<DiagnosticImagingTypeRef> findById(Long diagnosticImagingTypeId) {
    return diagnosticImagingTypeJpaRepository
        .findById(diagnosticImagingTypeId)
        .map(e -> new DiagnosticImagingTypeRef(e.getId(), e.getName()));
  }
}
