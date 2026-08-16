package com.vetsoftware.app.daycare.application.port.out;

import com.vetsoftware.app.daycare.domain.DayCare;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import java.util.Optional;

public interface DayCareRepository {
    DayCare save(DayCare dayCare);

    Optional<DayCare> findById(Long id);

    Optional<DayCare> findByIdAndCompanyId(Long id, Long companyId);

    List<DayCare> findAll();

    PageResult<DayCare> findAllByAnimalIdAndCompanyId(Long animalId, Long companyId, String query,
            int page, int pageSize);

    void delete(Long id);

    int reactivate(Long id);
}
