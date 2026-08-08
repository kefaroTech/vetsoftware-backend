package com.vetsoftware.app.hospitalization.application.port.out;

import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import com.vetsoftware.app.hospitalization.application.dto.PageResult;
import java.util.List;
import java.util.Optional;

public interface HospitalizationRepository {
    Hospitalization save(Hospitalization hospitalization);

    Optional<Hospitalization> findById(Long id);

    Optional<Hospitalization> findByIdAndCompanyId(Long id, Long companyId);

    List<Hospitalization> findAll();

    PageResult<Hospitalization> findAllByAnimalId(Long animalId, String query, int page,
            int pageSize);

    void delete(Long id);

    int reactivate(Long id);
}
