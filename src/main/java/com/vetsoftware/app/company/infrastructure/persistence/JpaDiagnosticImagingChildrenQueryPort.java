package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.company.application.port.out.DiagnosticImagingChildrenQueryPort;
import com.vetsoftware.app.diagnosticimaging.infrastructure.persistence.DiagnosticImagingJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaDiagnosticImagingChildrenQueryPort implements DiagnosticImagingChildrenQueryPort {
  private final DiagnosticImagingJpaRepository jpaRepository;

  public JpaDiagnosticImagingChildrenQueryPort(DiagnosticImagingJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByCompanyId(Long parentId) {
    return jpaRepository.existsByCompany_Id(parentId);
  }
}
