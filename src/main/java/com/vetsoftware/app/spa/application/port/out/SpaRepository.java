package com.vetsoftware.app.spa.application.port.out;

import com.vetsoftware.app.spa.domain.Spa;
import java.util.List;
import java.util.Optional;

public interface SpaRepository {
    Spa save(Spa spa);
    Optional<Spa> findById(Long id);
    List<Spa> findAll();
    void delete(Long id);
}
