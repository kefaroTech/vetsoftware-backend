package com.vetsoftware.app.baserolepermission.application.usecase;

import com.vetsoftware.app.baserolepermission.application.command.UpdateBaseRolePermissionCommand;
import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import com.vetsoftware.app.baserolepermission.application.port.in.UpdateBaseRolePermissionUseCase;
import com.vetsoftware.app.baserolepermission.application.port.out.BasePermissionQueryPort;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRolePermissionRepository;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRoleQueryPort;
import com.vetsoftware.app.baserolepermission.domain.BasePermissionRef;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermission;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermissionNotFoundException;
import com.vetsoftware.app.baserolepermission.domain.BaseRoleRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "base.role.permission.update")
@Service
public class UpdateBaseRolePermissionService implements UpdateBaseRolePermissionUseCase {
    private final BaseRolePermissionRepository repository;
    private final BaseRoleQueryPort baseRoleQueryPort;
    private final BasePermissionQueryPort basePermissionQueryPort;

    public UpdateBaseRolePermissionService(BaseRolePermissionRepository repository,
            BaseRoleQueryPort baseRoleQueryPort, BasePermissionQueryPort basePermissionQueryPort) {
        this.repository = repository;
        this.baseRoleQueryPort = baseRoleQueryPort;
        this.basePermissionQueryPort = basePermissionQueryPort;
    }

    @Override
    @Transactional
    public BaseRolePermissionDto execute(UpdateBaseRolePermissionCommand command) {
        BaseRolePermission baseRolePermission = repository.findById(command.id())
                .orElseThrow(() -> new BaseRolePermissionNotFoundException(command.id()));
        BaseRoleRef baseRole = baseRoleQueryPort.findById(command.baseRoleId()).orElseThrow(
                () -> new IllegalArgumentException("BaseRole not found: " + command.baseRoleId()));
        BasePermissionRef basePermission = basePermissionQueryPort
                .findById(command.basePermissionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "BasePermission not found: " + command.basePermissionId()));
        baseRolePermission.update(baseRole, basePermission);
        return BaseRolePermissionDto.from(repository.save(baseRolePermission));
    }
}
