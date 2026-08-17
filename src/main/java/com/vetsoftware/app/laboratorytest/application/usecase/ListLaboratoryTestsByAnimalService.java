package com.vetsoftware.app.laboratorytest.application.usecase;

import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.laboratorytest.application.port.in.ListLaboratoryTestsByAnimalUseCase;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "laboratory.test.list.by.animal")
@Service
public class ListLaboratoryTestsByAnimalService implements ListLaboratoryTestsByAnimalUseCase {
    private final LaboratoryTestRepository repository;

    public ListLaboratoryTestsByAnimalService(LaboratoryTestRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<LaboratoryTestDto> listByAnimal(Long animalId, Long companyId, String query,
            int page, int pageSize) {
        return repository.findAllByAnimalIdAndCompanyId(animalId, companyId, query, page, pageSize)
                .map(LaboratoryTestDto::from);
    }
}
