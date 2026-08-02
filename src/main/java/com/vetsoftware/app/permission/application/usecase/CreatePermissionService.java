package com.vetsoftware.app.permission.application.usecase;

import com.vetsoftware.app.permission.application.command.CreatePermissionCommand;
import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.port.in.CreatePermissionUseCase;
import com.vetsoftware.app.permission.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.permission.domain.CompanyRef;
import com.vetsoftware.app.permission.domain.Permission;
import com.vetsoftware.app.permission.domain.SubModuleRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "permission.create")
@Service
public class CreatePermissionService implements CreatePermissionUseCase {
    private final PermissionRepository repository;
    private final CompanyQueryPort companyQueryPort;
    private final SubModuleQueryPort subModuleQueryPort;

    public CreatePermissionService(PermissionRepository repository,
            CompanyQueryPort companyQueryPort, SubModuleQueryPort subModuleQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
        this.subModuleQueryPort = subModuleQueryPort;
    }

    @Override
    public PermissionDto execute(CreatePermissionCommand command) {
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
        SubModuleRef subModule = subModuleQueryPort.findById(command.subModuleId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "SubModule not found: " + command.subModuleId()));
        Permission permission = Permission.create(command.name(), command.code(), company,
                subModule);
        return PermissionDto.from(repository.save(permission));
    }
}
