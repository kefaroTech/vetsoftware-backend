package com.vetsoftware.app.consultation.infrastructure.persistence;

import com.vetsoftware.app.consultation.application.port.out.DiagnosticImagingChildrenQueryPort;
import com.vetsoftware.app.diagnosticimaging.infrastructure.persistence.DiagnosticImagingJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaDiagnosticImagingChildrenQueryPort implements DiagnosticImagingChildrenQueryPort {
    private final DiagnosticImagingJpaRepository jpaRepository;

    public JpaDiagnosticImagingChildrenQueryPort(DiagnosticImagingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByConsultationId(Long parentId) {
        return jpaRepository.existsByConsultation_Id(parentId);
    }
}
