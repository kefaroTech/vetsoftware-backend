package com.vetsoftware.app.medicamentprescription.infrastructure.persistence;

import com.vetsoftware.app.medicament.infrastructure.persistence.MedicamentJpaRepository;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentQueryPort;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaMedicamentQueryPort implements MedicamentQueryPort {
    private final MedicamentJpaRepository medicamentJpaRepository;

    public JpaMedicamentQueryPort(MedicamentJpaRepository medicamentJpaRepository) {
        this.medicamentJpaRepository = medicamentJpaRepository;
    }

    @Override
    public Optional<MedicamentRef> findById(Long medicamentId) {
        return medicamentJpaRepository.findById(medicamentId)
                .map(e -> new MedicamentRef(e.getId(), e.getName()));
    }
}
