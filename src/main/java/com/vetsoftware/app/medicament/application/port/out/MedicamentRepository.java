package com.vetsoftware.app.medicament.application.port.out;

import com.vetsoftware.app.medicament.domain.Medicament;
import java.util.List;
import java.util.Optional;

public interface MedicamentRepository {
    Medicament save(Medicament medicament);
    Optional<Medicament> findById(Long id);
    Optional<Medicament> findByIdAndCompanyId(Long id, Long companyId);
    List<Medicament> findAll();
    List<Medicament> findAllAvailableForCompany(Long companyId);
    List<Medicament> findAllDisabledForCompany(Long companyId);
    void delete(Long id);
    int reactivate(Long id);
}
