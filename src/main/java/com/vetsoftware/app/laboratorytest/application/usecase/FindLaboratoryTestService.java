package com.vetsoftware.app.laboratorytest.application.usecase;

import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.port.in.FindLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "laboratory_test.find")
@Service
public class FindLaboratoryTestService implements FindLaboratoryTestUseCase {
    private final LaboratoryTestRepository repository;

    public FindLaboratoryTestService(LaboratoryTestRepository repository) {
        this.repository = repository;
    }

    @Override
    public LaboratoryTestDto findById(Long id) {
        return LaboratoryTestDto.from(repository.findById(id)
            .orElseThrow(() -> new LaboratoryTestNotFoundException(id)));
    }
}
