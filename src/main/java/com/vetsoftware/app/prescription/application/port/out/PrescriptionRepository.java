package com.vetsoftware.app.prescription.application.port.out;

import com.vetsoftware.app.prescription.domain.Prescription;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

public interface PrescriptionRepository {
    Prescription save(Prescription prescription);

    Optional<Prescription> findById(Long id);

    Optional<Prescription> findByIdAndCompanyId(Long id, Long companyId);

    PageResult<Prescription> findAll(int page, int pageSize);

    void delete(Long id);

    int reactivate(Long id);
}
