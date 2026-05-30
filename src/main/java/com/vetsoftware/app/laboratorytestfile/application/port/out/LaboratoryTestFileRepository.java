package com.vetsoftware.app.laboratorytestfile.application.port.out;

import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFile;
import java.util.List;
import java.util.Optional;

public interface LaboratoryTestFileRepository {
    LaboratoryTestFile save(LaboratoryTestFile file);
    Optional<LaboratoryTestFile> findById(Long id);
    List<LaboratoryTestFile> findAllByLaboratoryTestId(Long laboratoryTestId);
    void delete(Long id);
}
