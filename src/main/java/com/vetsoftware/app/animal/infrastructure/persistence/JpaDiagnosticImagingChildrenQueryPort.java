package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.application.port.out.DiagnosticImagingChildrenQueryPort;
import com.vetsoftware.app.diagnosticimaging.infrastructure.persistence.DiagnosticImagingJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaDiagnosticImagingChildrenQueryPort implements DiagnosticImagingChildrenQueryPort {
    private final DiagnosticImagingJpaRepository jpaRepository;

    public JpaDiagnosticImagingChildrenQueryPort(DiagnosticImagingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByAnimalId(Long parentId) {
        return jpaRepository.existsByAnimal_Id(parentId);
    }
}
