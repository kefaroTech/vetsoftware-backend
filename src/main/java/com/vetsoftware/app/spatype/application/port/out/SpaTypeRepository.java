package com.vetsoftware.app.spatype.application.port.out;

import com.vetsoftware.app.spatype.domain.SpaType;
import java.util.List;
import java.util.Optional;

public interface SpaTypeRepository {
    SpaType save(SpaType spaType);

    Optional<SpaType> findById(Long id);

    List<SpaType> findAll();

    void delete(Long id);

    int reactivate(Long id);
}
