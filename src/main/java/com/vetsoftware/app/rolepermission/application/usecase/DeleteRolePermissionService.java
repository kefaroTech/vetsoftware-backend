package com.vetsoftware.app.rolepermission.application.usecase;

import com.vetsoftware.app.rolepermission.application.port.in.DeleteRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionCachePort;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import com.vetsoftware.app.rolepermission.domain.RolePermission;
import com.vetsoftware.app.rolepermission.domain.RolePermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "role.permission.delete")
@Service
public class DeleteRolePermissionService implements DeleteRolePermissionUseCase {
    private final RolePermissionRepository repository;
    private final PermissionCachePort permissionCachePort;

    public DeleteRolePermissionService(RolePermissionRepository repository,
                                       PermissionCachePort permissionCachePort) {
        this.repository = repository;
        this.permissionCachePort = permissionCachePort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        RolePermission rolePermission = repository.findById(id)
            .orElseThrow(() -> new RolePermissionNotFoundException(id));
        repository.delete(id);
        permissionCachePort.evictByRoleId(rolePermission.getRole().id());
    }
}
