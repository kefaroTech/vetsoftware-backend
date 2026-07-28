package com.vetsoftware.app.systempermission.application.usecase;

import com.vetsoftware.app.systempermission.application.port.in.DeleteSystemPermissionUseCase;
import com.vetsoftware.app.systempermission.application.port.out.SystemPermissionRepository;
import com.vetsoftware.app.systempermission.application.port.out.SystemUserPermissionChildrenQueryPort;
import com.vetsoftware.app.systempermission.domain.SystemPermissionHasActiveChildrenException;
import com.vetsoftware.app.systempermission.domain.SystemPermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "system.permission.delete")
@Service
public class DeleteSystemPermissionService implements DeleteSystemPermissionUseCase {
    private final SystemPermissionRepository repository;
    private final SystemUserPermissionChildrenQueryPort systemUserPermissionChildrenQueryPort;

    public DeleteSystemPermissionService(
            SystemPermissionRepository repository,
            SystemUserPermissionChildrenQueryPort systemUserPermissionChildrenQueryPort) {
        this.repository = repository;
        this.systemUserPermissionChildrenQueryPort = systemUserPermissionChildrenQueryPort;
    }

    @Override
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new SystemPermissionNotFoundException(id));
        if (systemUserPermissionChildrenQueryPort.existsActiveBySystemPermissionId(id)) {
            throw new SystemPermissionHasActiveChildrenException(id, "systemUserPermission");
        }
        repository.delete(id);
    }
}
