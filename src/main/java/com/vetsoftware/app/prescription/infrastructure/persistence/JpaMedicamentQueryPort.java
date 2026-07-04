package com.vetsoftware.app.prescription.infrastructure.persistence;

import com.vetsoftware.app.medicamentprescription.infrastructure.persistence.MedicamentPrescriptionJpaRepository;
import com.vetsoftware.app.prescription.application.port.out.MedicamentQueryPort;
import com.vetsoftware.app.prescription.domain.MedicamentRef;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JpaMedicamentQueryPort implements MedicamentQueryPort {
    private final MedicamentPrescriptionJpaRepository repository;

    public JpaMedicamentQueryPort(MedicamentPrescriptionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MedicamentRef> findByPrescriptionId(Long prescriptionId) {
        return repository.findByPrescriptionId(prescriptionId).stream()
                .map(e -> new MedicamentRef(
                        e.getId(),
                        e.getName(),
                        e.getPresentation(),
                        e.getQuantity(),
                        e.getPosology(),
                        e.getObservation()))
                .toList();
    }
}
