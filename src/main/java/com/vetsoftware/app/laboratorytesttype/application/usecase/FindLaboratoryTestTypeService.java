package com.vetsoftware.app.laboratorytesttype.application.usecase;

import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import com.vetsoftware.app.laboratorytesttype.application.port.in.FindLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "laboratory_test_type.find")
@Service
public class FindLaboratoryTestTypeService implements FindLaboratoryTestTypeUseCase {
    private final LaboratoryTestTypeRepository repository;

    public FindLaboratoryTestTypeService(LaboratoryTestTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public LaboratoryTestTypeDto findById(Long id) {
        return LaboratoryTestTypeDto.from(repository.findById(id)
                .orElseThrow(() -> new LaboratoryTestTypeNotFoundException(id)));
    }
}
