package com.vetsoftware.app.systemuserpermission.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.systemuserpermission.application.command.CreateSystemUserPermissionCommand;
import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import com.vetsoftware.app.systemuserpermission.application.port.in.CreateSystemUserPermissionUseCase;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemPermissionQueryPort;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserPermissionRepository;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserQueryPort;
import com.vetsoftware.app.systemuserpermission.domain.SystemPermissionRef;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermission;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "systemuserpermission.create")
@Service
public class CreateSystemUserPermissionService implements CreateSystemUserPermissionUseCase {
    private final SystemUserPermissionRepository repository;
    private final SystemUserQueryPort systemUserQueryPort;
    private final SystemPermissionQueryPort systemPermissionQueryPort;

    public CreateSystemUserPermissionService(SystemUserPermissionRepository repository,
                                             SystemUserQueryPort systemUserQueryPort,
                                             SystemPermissionQueryPort systemPermissionQueryPort) {
        this.repository = repository;
        this.systemUserQueryPort = systemUserQueryPort;
        this.systemPermissionQueryPort = systemPermissionQueryPort;
    }

    @Override
    public SystemUserPermissionDto execute(CreateSystemUserPermissionCommand command, AuthContext auth) {
        SystemUserRef systemUser = systemUserQueryPort.findById(command.systemUserId())
            .orElseThrow(() -> new IllegalArgumentException("SystemUser not found: " + command.systemUserId()));
        SystemPermissionRef systemPermission = systemPermissionQueryPort.findById(command.systemPermissionId())
            .orElseThrow(() -> new IllegalArgumentException("SystemPermission not found: " + command.systemPermissionId()));
        SystemUserPermission systemUserPermission = SystemUserPermission.create(systemUser, systemPermission);
        return SystemUserPermissionDto.from(repository.save(systemUserPermission));
    }
}
