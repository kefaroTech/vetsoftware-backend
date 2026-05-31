package com.vetsoftware.app.laboratorytest.application.usecase;

import com.vetsoftware.app.laboratorytest.application.command.SearchLaboratoryTestsCommand;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.dto.PageResult;
import com.vetsoftware.app.laboratorytest.application.port.in.SearchLaboratoryTestsUseCase;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "laboratory_test.search")
@Service
public class SearchLaboratoryTestsService implements SearchLaboratoryTestsUseCase {
    private final LaboratoryTestRepository repository;

    public SearchLaboratoryTestsService(LaboratoryTestRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<LaboratoryTestDto> execute(SearchLaboratoryTestsCommand command) {
        return repository.search(command).map(LaboratoryTestDto::from);
    }
}
