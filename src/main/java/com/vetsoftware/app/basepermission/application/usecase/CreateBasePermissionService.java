package com.vetsoftware.app.basepermission.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.basepermission.application.command.CreateBasePermissionCommand;
import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import com.vetsoftware.app.basepermission.application.port.in.CreateBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.out.BasePermissionRepository;
import com.vetsoftware.app.basepermission.application.port.out.SubModuleValidationPort;
import com.vetsoftware.app.basepermission.domain.BasePermission;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "basepermission.create")
@Service
public class CreateBasePermissionService implements CreateBasePermissionUseCase {
    private final BasePermissionRepository repository;
    private final SubModuleValidationPort subModuleValidationPort;

    public CreateBasePermissionService(BasePermissionRepository repository,
                                       SubModuleValidationPort subModuleValidationPort) {
        this.repository = repository;
        this.subModuleValidationPort = subModuleValidationPort;
    }

    @Override
    public BasePermissionDto execute(CreateBasePermissionCommand command, AuthContext auth) {
        subModuleValidationPort.validateExists(command.subModuleId());
        BasePermission basePermission = BasePermission.create(command.name(), command.code(), command.subModuleId());
        return BasePermissionDto.from(repository.save(basePermission));
    }
}
