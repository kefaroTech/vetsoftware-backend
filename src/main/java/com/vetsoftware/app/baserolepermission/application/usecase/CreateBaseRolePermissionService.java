package com.vetsoftware.app.baserolepermission.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.baserolepermission.application.command.CreateBaseRolePermissionCommand;
import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import com.vetsoftware.app.baserolepermission.application.port.in.CreateBaseRolePermissionUseCase;
import com.vetsoftware.app.baserolepermission.application.port.out.BasePermissionValidationPort;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRolePermissionRepository;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRoleValidationPort;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermission;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "baserolepermission.create")
@Service
public class CreateBaseRolePermissionService implements CreateBaseRolePermissionUseCase {
    private final BaseRolePermissionRepository repository;
    private final BaseRoleValidationPort baseRoleValidationPort;
    private final BasePermissionValidationPort basePermissionValidationPort;

    public CreateBaseRolePermissionService(BaseRolePermissionRepository repository,
                                            BaseRoleValidationPort baseRoleValidationPort,
                                            BasePermissionValidationPort basePermissionValidationPort) {
        this.repository = repository;
        this.baseRoleValidationPort = baseRoleValidationPort;
        this.basePermissionValidationPort = basePermissionValidationPort;
    }

    @Override
    public BaseRolePermissionDto execute(CreateBaseRolePermissionCommand command, AuthContext auth) {
        baseRoleValidationPort.validateExists(command.baseRoleId());
        basePermissionValidationPort.validateExists(command.basePermissionId());
        BaseRolePermission baseRolePermission = BaseRolePermission.create(command.baseRoleId(), command.basePermissionId());
        return BaseRolePermissionDto.from(repository.save(baseRolePermission));
    }
}
