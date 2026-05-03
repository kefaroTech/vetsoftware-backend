package com.vetsoftware.app.testtype.application.usecase;

import com.vetsoftware.app.testtype.application.command.CreateTestTypeCommand;
import com.vetsoftware.app.testtype.application.dto.TestTypeDto;
import com.vetsoftware.app.testtype.application.port.in.CreateTestTypeUseCase;
import com.vetsoftware.app.testtype.application.port.out.TestTypeRepository;
import com.vetsoftware.app.testtype.domain.TestType;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "test_type.create")
@Service
public class CreateTestTypeService implements CreateTestTypeUseCase {
    private final TestTypeRepository repository;

    public CreateTestTypeService(TestTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public TestTypeDto execute(CreateTestTypeCommand command) {
        return TestTypeDto.from(
                repository.save(TestType.create(command.name(), command.description())));
    }
}
