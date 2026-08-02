package com.vetsoftware.app.spa.application.port.out;

import com.vetsoftware.app.spa.domain.Spa;
import java.util.List;
import java.util.Optional;

public interface SpaRepository {
    Spa save(Spa spa);

    Optional<Spa> findById(Long id);

    Optional<Spa> findByIdAndCompanyId(Long id, Long companyId);

    List<Spa> findAll();

    List<Spa> findAllByAnimalId(Long animalId);

    void delete(Long id);

    int reactivate(Long id);
}
