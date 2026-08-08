package com.vetsoftware.app.owner.application.port.out;

import com.vetsoftware.app.owner.application.dto.PageResult;
import com.vetsoftware.app.owner.domain.Owner;
import java.util.Optional;

public interface OwnerRepository {
    Owner save(Owner owner);

    Optional<Owner> findByIdAndCompanyId(Long id, Long companyId);

    PageResult<Owner> findAllByCompanyId(Long companyId, int page, int pageSize);

    PageResult<Owner> searchByCompanyAndTerm(Long companyId, String query, int page, int pageSize);

    void delete(Long id, Long companyId);

    int reactivate(Long id, Long companyId);
}
