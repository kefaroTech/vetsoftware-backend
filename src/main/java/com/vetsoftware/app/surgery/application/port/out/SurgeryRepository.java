package com.vetsoftware.app.surgery.application.port.out;

import com.vetsoftware.app.surgery.domain.Surgery;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import java.util.Optional;

public interface SurgeryRepository {
    Surgery save(Surgery surgery);

    Optional<Surgery> findById(Long id);

    Optional<Surgery> findByIdAndCompanyId(Long id, Long companyId);

    List<Surgery> findAll();

    PageResult<Surgery> findAllByAnimalIdAndCompanyId(Long animalId, Long companyId, String query,
            int page, int pageSize);

    void delete(Long id);
}
