package com.vetsoftware.app.surgery.application.port.out;

import com.vetsoftware.app.surgery.domain.Surgery;
import java.util.List;
import java.util.Optional;

public interface SurgeryRepository {
    Surgery save(Surgery surgery);

    Optional<Surgery> findById(Long id);

    Optional<Surgery> findByIdAndCompanyId(Long id, Long companyId);

    List<Surgery> findAll();

    List<Surgery> findAllByAnimalId(Long animalId);

    void delete(Long id);

    int reactivate(Long id);
}
