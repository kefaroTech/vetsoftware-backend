package com.vetsoftware.app.vaccination.application.port.out;

import com.vetsoftware.app.vaccination.domain.Vaccination;
import java.util.List;
import java.util.Optional;

public interface VaccinationRepository {
    Vaccination save(Vaccination vaccination);
    Optional<Vaccination> findById(Long id);
    Optional<Vaccination> findByIdAndCompanyId(Long id, Long companyId);
    List<Vaccination> findAll();
    List<Vaccination> findAllByAnimalId(Long animalId);
    void delete(Long id);
    int reactivate(Long id);
}
