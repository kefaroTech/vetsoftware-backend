package com.vetsoftware.app.testtype.application.usecase;

import com.vetsoftware.app.testtype.application.command.CreateTestTypeCommand;
import com.vetsoftware.app.testtype.application.dto.TestTypeDto;
import com.vetsoftware.app.testtype.application.port.in.CreateTestTypeUseCase;
import com.vetsoftware.app.testtype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.testtype.application.port.out.TestTypeRepository;
import com.vetsoftware.app.testtype.domain.CompanyRef;
import com.vetsoftware.app.testtype.domain.TestType;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "test_type.create")
@Service
public class CreateTestTypeService implements CreateTestTypeUseCase {
    private final TestTypeRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public CreateTestTypeService(TestTypeRepository repository,
                                 CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public TestTypeDto execute(CreateTestTypeCommand command) {
        CompanyRef company = command.companyId() == null ? null
                : companyQueryPort.findById(command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        return TestTypeDto.from(
                repository.save(TestType.create(command.name(), command.description(), company, command.general())));
    }
}
