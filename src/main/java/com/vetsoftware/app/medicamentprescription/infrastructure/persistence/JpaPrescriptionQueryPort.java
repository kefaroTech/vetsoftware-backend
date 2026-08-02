package com.vetsoftware.app.medicamentprescription.infrastructure.persistence;

import com.vetsoftware.app.medicamentprescription.application.port.out.PrescriptionQueryPort;
import com.vetsoftware.app.medicamentprescription.domain.PrescriptionRef;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("medicamentPrescriptionJpaPrescriptionQueryPort")
public class JpaPrescriptionQueryPort implements PrescriptionQueryPort {
    private final PrescriptionJpaRepository prescriptionJpaRepository;

    public JpaPrescriptionQueryPort(PrescriptionJpaRepository prescriptionJpaRepository) {
        this.prescriptionJpaRepository = prescriptionJpaRepository;
    }

    @Override
    public Optional<PrescriptionRef> findById(Long prescriptionId) {
        return prescriptionJpaRepository.findById(prescriptionId)
                .map(e -> new PrescriptionRef(e.getId(), e.getDate()));
    }

    @Override
    public Optional<PrescriptionRef> findByIdAndCompanyId(Long prescriptionId, Long companyId) {
        return prescriptionJpaRepository.findByIdAndCompany_Id(prescriptionId, companyId)
                .map(e -> new PrescriptionRef(e.getId(), e.getDate()));
    }
}
