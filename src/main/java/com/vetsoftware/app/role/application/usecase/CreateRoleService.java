package com.vetsoftware.app.role.application.usecase;

import com.vetsoftware.app.role.application.command.CreateRoleCommand;
import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.in.CreateRoleUseCase;
import com.vetsoftware.app.role.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.domain.CompanyRef;
import com.vetsoftware.app.role.domain.Role;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "role.create")
@Service
public class CreateRoleService implements CreateRoleUseCase {
    private final RoleRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public CreateRoleService(RoleRepository repository, CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public RoleDto execute(CreateRoleCommand command) {
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
        Role role = Role.create(command.name(), command.code(), company);
        return RoleDto.from(repository.save(role));
    }
}
