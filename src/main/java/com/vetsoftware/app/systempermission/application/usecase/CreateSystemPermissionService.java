package com.vetsoftware.app.systempermission.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.systempermission.application.command.CreateSystemPermissionCommand;
import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import com.vetsoftware.app.systempermission.application.port.in.CreateSystemPermissionUseCase;
import com.vetsoftware.app.systempermission.application.port.out.SystemPermissionRepository;
import com.vetsoftware.app.systempermission.domain.SystemPermission;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "systempermission.create")
@Service
public class CreateSystemPermissionService implements CreateSystemPermissionUseCase {
    private final SystemPermissionRepository repository;

    public CreateSystemPermissionService(SystemPermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public SystemPermissionDto execute(CreateSystemPermissionCommand command, AuthContext auth) {
        SystemPermission systemPermission = SystemPermission.create(command.name(), command.code());
        return SystemPermissionDto.from(repository.save(systemPermission));
    }
}
