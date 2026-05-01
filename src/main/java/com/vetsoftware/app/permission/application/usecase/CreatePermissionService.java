package com.vetsoftware.app.permission.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.permission.application.command.CreatePermissionCommand;
import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.port.in.CreatePermissionUseCase;
import com.vetsoftware.app.permission.application.port.out.CompanyValidationPort;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.application.port.out.SubModuleValidationPort;
import com.vetsoftware.app.permission.domain.Permission;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "permission.create")
@Service
public class CreatePermissionService implements CreatePermissionUseCase {
    private final PermissionRepository repository;
    private final CompanyValidationPort companyValidationPort;
    private final SubModuleValidationPort subModuleValidationPort;

    public CreatePermissionService(PermissionRepository repository,
                                    CompanyValidationPort companyValidationPort,
                                    SubModuleValidationPort subModuleValidationPort) {
        this.repository = repository;
        this.companyValidationPort = companyValidationPort;
        this.subModuleValidationPort = subModuleValidationPort;
    }

    @Override
    public PermissionDto execute(CreatePermissionCommand command, AuthContext auth) {
        companyValidationPort.validateExists(command.companyId());
        subModuleValidationPort.validateExists(command.subModuleId());
        Permission permission = Permission.create(command.name(), command.code(), command.companyId(), command.subModuleId());
        return PermissionDto.from(repository.save(permission));
    }
}
