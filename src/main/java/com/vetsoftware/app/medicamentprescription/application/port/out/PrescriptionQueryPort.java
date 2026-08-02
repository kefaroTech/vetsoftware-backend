package com.vetsoftware.app.medicamentprescription.application.port.out;

import com.vetsoftware.app.medicamentprescription.domain.PrescriptionRef;
import java.util.Optional;

public interface PrescriptionQueryPort {
    Optional<PrescriptionRef> findById(Long prescriptionId);
    Optional<PrescriptionRef> findByIdAndCompanyId(Long prescriptionId, Long companyId);
}
