package com.vetsoftware.app.animal.application.port.out;

import com.vetsoftware.app.animal.domain.WeightRecord;
import java.util.List;
import java.util.Optional;

public interface WeightRecordRepository {
    WeightRecord save(WeightRecord weightRecord);

    /** Serie temporal del animal, más reciente primero. */
    List<WeightRecord> findByAnimalIdAndCompanyId(Long animalId, Long companyId);

    Optional<WeightRecord> findLatestByAnimalIdAndCompanyId(Long animalId, Long companyId);

    Optional<WeightRecord> findByIdAndAnimalIdAndCompanyId(Long id, Long animalId, Long companyId);

    void delete(Long id, Long companyId);
}
