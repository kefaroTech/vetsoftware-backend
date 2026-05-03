package com.vetsoftware.app.owner.application.port.out;

import com.vetsoftware.app.owner.domain.Owner;
import java.util.List;
import java.util.Optional;

public interface OwnerRepository {
    Owner save(Owner owner);
    Optional<Owner> findById(Long id);
    List<Owner> findAll();
    void delete(Long id);
}
