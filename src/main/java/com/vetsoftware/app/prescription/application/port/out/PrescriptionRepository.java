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

    /** Sin acotar: solo el camino SYSTEM ({@code companyId == null}). */
    int reactivate(Long id);

    /** Acotado a la empresa. */
    int reactivate(Long id, Long companyId);
}
