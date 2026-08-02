package com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence;

import com.vetsoftware.app.diagnosticimaging.infrastructure.persistence.DiagnosticImagingJpaRepository;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaDiagnosticImagingChildrenQueryPort implements DiagnosticImagingChildrenQueryPort {
  private final DiagnosticImagingJpaRepository jpaRepository;

  public JpaDiagnosticImagingChildrenQueryPort(DiagnosticImagingJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByDiagnosticImagingTypeId(Long parentId) {
    return jpaRepository.existsByDiagnosticImagingType_Id(parentId);
  }
}
