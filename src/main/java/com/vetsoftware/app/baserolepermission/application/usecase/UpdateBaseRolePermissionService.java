package com.vetsoftware.app.baserolepermission.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.baserolepermission.application.command.UpdateBaseRolePermissionCommand;
import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import com.vetsoftware.app.baserolepermission.application.port.in.UpdateBaseRolePermissionUseCase;
import com.vetsoftware.app.baserolepermission.application.port.out.BasePermissionValidationPort;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRolePermissionRepository;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRoleValidationPort;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermission;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "baserolepermission.update")
@Service
public class UpdateBaseRolePermissionService implements UpdateBaseRolePermissionUseCase {
    private final BaseRolePermissionRepository repository;
    private final BaseRoleValidationPort baseRoleValidationPort;
    private final BasePermissionValidationPort basePermissionValidationPort;

    public UpdateBaseRolePermissionService(BaseRolePermissionRepository repository,
                                            BaseRoleValidationPort baseRoleValidationPort,
                                            BasePermissionValidationPort basePermissionValidationPort) {
        this.repository = repository;
        this.baseRoleValidationPort = baseRoleValidationPort;
        this.basePermissionValidationPort = basePermissionValidationPort;
    }

    @Override
    @Transactional
    public BaseRolePermissionDto execute(UpdateBaseRolePermissionCommand command, AuthContext auth) {
        BaseRolePermission baseRolePermission = repository.findById(command.id())
            .orElseThrow(() -> new BaseRolePermissionNotFoundException(command.id()));
        baseRoleValidationPort.validateExists(command.baseRoleId());
        basePermissionValidationPort.validateExists(command.basePermissionId());
        baseRolePermission.update(command.baseRoleId(), command.basePermissionId());
        return BaseRolePermissionDto.from(repository.save(baseRolePermission));
    }
}
