package com.vetsoftware.app.hospitalization.application.port.out;

import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import java.util.List;
import java.util.Optional;

public interface HospitalizationRepository {
    Hospitalization save(Hospitalization hospitalization);
    Optional<Hospitalization> findById(Long id);
    List<Hospitalization> findAll();
    void delete(Long id);
}
