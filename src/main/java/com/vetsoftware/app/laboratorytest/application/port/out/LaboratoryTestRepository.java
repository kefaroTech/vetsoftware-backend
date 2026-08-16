package com.vetsoftware.app.laboratorytest.application.port.out;

import com.vetsoftware.app.laboratorytest.application.command.SearchLaboratoryTestsCommand;
import com.vetsoftware.app.laboratorytest.application.dto.PageResult;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import java.util.List;
import java.util.Optional;

public interface LaboratoryTestRepository {
    LaboratoryTest save(LaboratoryTest laboratoryTest);

    Optional<LaboratoryTest> findById(Long id);

    Optional<LaboratoryTest> findByIdAndCompanyId(Long id, Long companyId);

    List<LaboratoryTest> findAll();

    PageResult<LaboratoryTest> findAllByAnimalIdAndCompanyId(Long animalId, Long companyId,
            String query, int page, int pageSize);

    PageResult<LaboratoryTest> search(SearchLaboratoryTestsCommand command);

    void delete(Long id);

    int reactivate(Long id);
}
