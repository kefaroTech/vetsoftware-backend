package com.vetsoftware.app.vaccination.application.port.out;

import com.vetsoftware.app.vaccination.domain.Vaccination;
import com.vetsoftware.app.vaccination.application.dto.PageResult;
import java.util.List;
import java.util.Optional;

public interface VaccinationRepository {
    Vaccination save(Vaccination vaccination);

    Optional<Vaccination> findById(Long id);

    Optional<Vaccination> findByIdAndCompanyId(Long id, Long companyId);

    List<Vaccination> findAll();

    PageResult<Vaccination> findAllByAnimalIdAndCompanyId(Long animalId, Long companyId,
            String query, int page, int pageSize);

    void delete(Long id);

    int reactivate(Long id);
}
