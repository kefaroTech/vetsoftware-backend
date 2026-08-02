package com.vetsoftware.app.hospitalization.application.port.out;

import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import java.util.List;
import java.util.Optional;

public interface HospitalizationRepository {
    Hospitalization save(Hospitalization hospitalization);
    Optional<Hospitalization> findById(Long id);
    Optional<Hospitalization> findByIdAndCompanyId(Long id, Long companyId);
    List<Hospitalization> findAll();
    List<Hospitalization> findAllByAnimalId(Long animalId);
    void delete(Long id);
    int reactivate(Long id);
}
