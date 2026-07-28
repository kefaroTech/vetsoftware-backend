package com.vetsoftware.app.systemuserpermission.application.usecase;

import com.vetsoftware.app.systemuserpermission.application.command.UpdateSystemUserPermissionCommand;
import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import com.vetsoftware.app.systemuserpermission.application.port.in.UpdateSystemUserPermissionUseCase;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemPermissionQueryPort;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserPermissionRepository;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserQueryPort;
import com.vetsoftware.app.systemuserpermission.domain.SystemPermissionRef;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermission;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermissionNotFoundException;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "system.user.permission.update")
@Service
public class UpdateSystemUserPermissionService implements UpdateSystemUserPermissionUseCase {
    private final SystemUserPermissionRepository repository;
    private final SystemUserQueryPort systemUserQueryPort;
    private final SystemPermissionQueryPort systemPermissionQueryPort;

    public UpdateSystemUserPermissionService(SystemUserPermissionRepository repository,
                                             SystemUserQueryPort systemUserQueryPort,
                                             SystemPermissionQueryPort systemPermissionQueryPort) {
        this.repository = repository;
        this.systemUserQueryPort = systemUserQueryPort;
        this.systemPermissionQueryPort = systemPermissionQueryPort;
    }

    @Override
    @Transactional
    public SystemUserPermissionDto execute(UpdateSystemUserPermissionCommand command) {
        SystemUserPermission systemUserPermission = repository.findById(command.id())
            .orElseThrow(() -> new SystemUserPermissionNotFoundException(command.id()));
        SystemUserRef systemUser = systemUserQueryPort.findById(command.systemUserId())
            .orElseThrow(() -> new IllegalArgumentException("SystemUser not found: " + command.systemUserId()));
        SystemPermissionRef systemPermission = systemPermissionQueryPort.findById(command.systemPermissionId())
            .orElseThrow(() -> new IllegalArgumentException("SystemPermission not found: " + command.systemPermissionId()));
        systemUserPermission.update(systemUser, systemPermission);
        return SystemUserPermissionDto.from(repository.save(systemUserPermission));
    }
}
