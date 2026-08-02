package com.vetsoftware.app.role.application.usecase;

import com.vetsoftware.app.role.application.command.UpdateRoleCommand;
import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.in.UpdateRoleUseCase;
import com.vetsoftware.app.role.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.domain.CompanyRef;
import com.vetsoftware.app.role.domain.Role;
import com.vetsoftware.app.role.domain.RoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "role.update")
@Service
public class UpdateRoleService implements UpdateRoleUseCase {
    private final RoleRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public UpdateRoleService(RoleRepository repository, CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public RoleDto execute(UpdateRoleCommand command) {
        Role role = repository.findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new RoleNotFoundException(command.id()));
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
        role.update(command.name(), command.code(), company);
        return RoleDto.from(repository.save(role));
    }
}
