package com.vetsoftware.app.testtype.application.usecase;

import com.vetsoftware.app.testtype.application.dto.TestTypeDto;
import com.vetsoftware.app.testtype.application.port.in.FindTestTypeUseCase;
import com.vetsoftware.app.testtype.application.port.out.TestTypeRepository;
import com.vetsoftware.app.testtype.domain.TestTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "test_type.find")
@Service
public class FindTestTypeService implements FindTestTypeUseCase {
    private final TestTypeRepository repository;

    public FindTestTypeService(TestTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public TestTypeDto findById(Long id) {
        return TestTypeDto.from(repository.findById(id)
                .orElseThrow(() -> new TestTypeNotFoundException(id)));
    }
}
