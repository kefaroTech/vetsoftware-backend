package com.vetsoftware.app.testtype.application.usecase;

import com.vetsoftware.app.testtype.application.command.UpdateTestTypeCommand;
import com.vetsoftware.app.testtype.application.dto.TestTypeDto;
import com.vetsoftware.app.testtype.application.port.in.UpdateTestTypeUseCase;
import com.vetsoftware.app.testtype.application.port.out.TestTypeRepository;
import com.vetsoftware.app.testtype.domain.TestType;
import com.vetsoftware.app.testtype.domain.TestTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "test_type.update")
@Service
public class UpdateTestTypeService implements UpdateTestTypeUseCase {
    private final TestTypeRepository repository;

    public UpdateTestTypeService(TestTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public TestTypeDto execute(UpdateTestTypeCommand command) {
        TestType testType = repository.findById(command.id())
                .orElseThrow(() -> new TestTypeNotFoundException(command.id()));
        testType.update(command.name(), command.description());
        return TestTypeDto.from(repository.save(testType));
    }
}
