package com.vetsoftware.app.laboratorytestfile.infrastructure.persistence;

import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaRepository;
import com.vetsoftware.app.laboratorytestfile.application.port.out.LaboratoryTestQueryPort;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("laboratoryTestFileJpaLaboratoryTestQueryPort")
public class JpaLaboratoryTestQueryPort implements LaboratoryTestQueryPort {
    private final LaboratoryTestJpaRepository laboratoryTestJpaRepository;

    public JpaLaboratoryTestQueryPort(LaboratoryTestJpaRepository laboratoryTestJpaRepository) {
        this.laboratoryTestJpaRepository = laboratoryTestJpaRepository;
    }

    @Override
    public Optional<LaboratoryTestRef> findById(Long laboratoryTestId) {
        return laboratoryTestJpaRepository.findById(laboratoryTestId)
            .map(e -> new LaboratoryTestRef(e.getId(), e.getDate()));
    }
}
