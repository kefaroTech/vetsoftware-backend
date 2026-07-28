package com.vetsoftware.app.laboratorytest.application.usecase;

import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.port.in.ListLaboratoryTestsUseCase;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "laboratory.test.list")
@Service
public class ListLaboratoryTestsService implements ListLaboratoryTestsUseCase {
    private final LaboratoryTestRepository repository;

    public ListLaboratoryTestsService(LaboratoryTestRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<LaboratoryTestDto> listAll() {
        return repository.findAll().stream().map(LaboratoryTestDto::from).toList();
    }
}
