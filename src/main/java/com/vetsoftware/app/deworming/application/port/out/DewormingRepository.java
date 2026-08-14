package com.vetsoftware.app.deworming.application.port.out;

import com.vetsoftware.app.deworming.domain.Deworming;
import com.vetsoftware.app.deworming.application.dto.PageResult;
import java.util.List;
import java.util.Optional;

public interface DewormingRepository {
    Deworming save(Deworming deworming);

    Optional<Deworming> findById(Long id);

    Optional<Deworming> findByIdAndCompanyId(Long id, Long companyId);

    List<Deworming> findAll();

    PageResult<Deworming> findAllByAnimalIdAndCompanyId(Long animalId, Long companyId, String query,
            int page, int pageSize);

    void delete(Long id);

    int reactivate(Long id);
}
