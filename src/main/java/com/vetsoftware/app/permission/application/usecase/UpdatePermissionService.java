package com.vetsoftware.app.permission.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.permission.application.command.UpdatePermissionCommand;
import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.port.in.UpdatePermissionUseCase;
import com.vetsoftware.app.permission.application.port.out.CompanyValidationPort;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.application.port.out.SubModuleValidationPort;
import com.vetsoftware.app.permission.domain.Permission;
import com.vetsoftware.app.permission.domain.PermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "permission.update")
@Service
public class UpdatePermissionService implements UpdatePermissionUseCase {
    private final PermissionRepository repository;
    private final CompanyValidationPort companyValidationPort;
    private final SubModuleValidationPort subModuleValidationPort;

    public UpdatePermissionService(PermissionRepository repository,
                                    CompanyValidationPort companyValidationPort,
                                    SubModuleValidationPort subModuleValidationPort) {
        this.repository = repository;
        this.companyValidationPort = companyValidationPort;
        this.subModuleValidationPort = subModuleValidationPort;
    }

    @Override
    @Transactional
    public PermissionDto execute(UpdatePermissionCommand command, AuthContext auth) {
        Permission permission = repository.findById(command.id())
            .orElseThrow(() -> new PermissionNotFoundException(command.id()));
        companyValidationPort.validateExists(command.companyId());
        subModuleValidationPort.validateExists(command.subModuleId());
        permission.update(command.name(), command.code(), command.companyId(), command.subModuleId());
        return PermissionDto.from(repository.save(permission));
    }
}
