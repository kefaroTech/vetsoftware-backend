package com.vetsoftware.app.company.infrastructure.orchestration;

import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.CreateCompanyUseCase;
import com.vetsoftware.app.company.application.port.out.CompanyCreationPort;
import org.springframework.stereotype.Component;

/** Conserva una sola primitiva de persistencia para company y registration. */
@Component
public class PrimitiveCompanyCreationAdapter implements CompanyCreationPort {

    private final CreateCompanyUseCase createCompanyUseCase;

    public PrimitiveCompanyCreationAdapter(CreateCompanyUseCase createCompanyUseCase) {
        this.createCompanyUseCase = createCompanyUseCase;
    }

    @Override
    public CompanyDto create(CreateCompanyCommand command) {
        return createCompanyUseCase.execute(command);
    }
}
