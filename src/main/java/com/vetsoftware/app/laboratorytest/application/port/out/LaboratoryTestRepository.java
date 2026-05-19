package com.vetsoftware.app.laboratorytest.application.port.out;

import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import java.util.List;
import java.util.Optional;

public interface LaboratoryTestRepository {
    LaboratoryTest save(LaboratoryTest laboratoryTest);
    Optional<LaboratoryTest> findById(Long id);
    List<LaboratoryTest> findAll();
    List<LaboratoryTest> findAllByAnimalId(Long animalId);
    void delete(Long id);
}
