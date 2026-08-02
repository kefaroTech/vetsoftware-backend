package com.vetsoftware.app.prescription.application.port.out;

import com.vetsoftware.app.prescription.domain.Prescription;
import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository {
    Prescription save(Prescription prescription);
    Optional<Prescription> findById(Long id);
    Optional<Prescription> findByIdAndCompanyId(Long id, Long companyId);
    List<Prescription> findAll();
    void delete(Long id);
    int reactivate(Long id);
}
