package com.vetsoftware.app.deworming.application.port.out;

import com.vetsoftware.app.deworming.domain.Deworming;
import java.util.List;
import java.util.Optional;

public interface DewormingRepository {
    Deworming save(Deworming deworming);

    Optional<Deworming> findById(Long id);

    Optional<Deworming> findByIdAndCompanyId(Long id, Long companyId);

    List<Deworming> findAll();

    List<Deworming> findAllByAnimalId(Long animalId);

    void delete(Long id);

    int reactivate(Long id);
}
