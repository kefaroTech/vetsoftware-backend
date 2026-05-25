package com.vetsoftware.app.daycare.application.port.out;

import com.vetsoftware.app.daycare.domain.DayCare;
import java.util.List;
import java.util.Optional;

public interface DayCareRepository {
    DayCare save(DayCare dayCare);
    Optional<DayCare> findById(Long id);
    List<DayCare> findAll();
    List<DayCare> findAllByAnimalId(Long animalId);
    void delete(Long id);
    int reactivate(Long id);
}
