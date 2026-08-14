package com.vetsoftware.app.laboratorytestfile.application.port.out;

import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFile;
import java.util.List;
import java.util.Optional;

public interface LaboratoryTestFileRepository {
    LaboratoryTestFile save(LaboratoryTestFile file);

    Optional<LaboratoryTestFile> findById(Long id);

    Optional<LaboratoryTestFile> findByIdAndCompanyId(Long id, Long companyId);

    List<LaboratoryTestFile> findAllByLaboratoryTestIdAndCompanyId(Long laboratoryTestId,
            Long companyId);

    void delete(Long id);
}
