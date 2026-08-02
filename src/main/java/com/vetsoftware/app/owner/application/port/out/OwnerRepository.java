package com.vetsoftware.app.owner.application.port.out;

import com.vetsoftware.app.owner.domain.Owner;
import java.util.List;
import java.util.Optional;

public interface OwnerRepository {
    Owner save(Owner owner);

    Optional<Owner> findByIdAndCompanyId(Long id, Long companyId);

    List<Owner> findAllByCompanyId(Long companyId);

    List<Owner> searchByCompanyAndTerm(Long companyId, String query);

    void delete(Long id, Long companyId);

    int reactivate(Long id, Long companyId);
}
