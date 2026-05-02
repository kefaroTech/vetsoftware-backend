package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.CreateCompanyUseCase;
import com.vetsoftware.app.registration.application.port.out.CompanyCreator;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CreateCompanyAdapter implements CompanyCreator {

    private static final AuthContext SYSTEM_CONTEXT =
        new AuthContext(null, Set.of("admin.all"));

    private final CreateCompanyUseCase createCompanyUseCase;

    public CreateCompanyAdapter(CreateCompanyUseCase createCompanyUseCase) {
        this.createCompanyUseCase = createCompanyUseCase;
    }

    @Override
    public CompanyResult create(String name, String identifier, String address,
                                String contactNumber, Long cityId) {
        CompanyDto dto = createCompanyUseCase.execute(
            new CreateCompanyCommand(name, identifier, address, contactNumber, cityId),
            SYSTEM_CONTEXT
        );
        return new CompanyResult(dto.id(), dto.name(), dto.identifier());
    }
}
