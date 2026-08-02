package com.vetsoftware.app.permission.application.usecase;

import com.vetsoftware.app.permission.application.port.in.DeletePermissionUseCase;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.application.port.out.RolePermissionChildrenQueryPort;
import com.vetsoftware.app.permission.domain.PermissionHasActiveChildrenException;
import com.vetsoftware.app.permission.domain.PermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "permission.delete")
@Service
public class DeletePermissionService implements DeletePermissionUseCase {
    private final PermissionRepository repository;
    private final RolePermissionChildrenQueryPort rolePermissionChildrenQueryPort;

    public DeletePermissionService(PermissionRepository repository,
            RolePermissionChildrenQueryPort rolePermissionChildrenQueryPort) {
        this.repository = repository;
        this.rolePermissionChildrenQueryPort = rolePermissionChildrenQueryPort;
    }

    @Override
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new PermissionNotFoundException(id));
        if (rolePermissionChildrenQueryPort.existsActiveByPermissionId(id)) {
            throw new PermissionHasActiveChildrenException(id, "rolePermission");
        }
        repository.delete(id);
    }
}
