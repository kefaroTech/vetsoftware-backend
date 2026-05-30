package com.vetsoftware.app.laboratorytestfile.infrastructure.persistence;

import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaRepository;
import com.vetsoftware.app.laboratorytestfile.application.port.out.LaboratoryTestQueryPort;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestRef;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestStoragePathRef;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional(readOnly = true)
    public Optional<LaboratoryTestStoragePathRef> findStoragePath(Long laboratoryTestId) {
        return laboratoryTestJpaRepository.findById(laboratoryTestId)
            .map(e -> new LaboratoryTestStoragePathRef(
                e.getCompany().getId(),
                e.getAnimal().getOwner().getId(),
                e.getAnimal().getId(),
                e.getAnimal().getName()));
    }
}
