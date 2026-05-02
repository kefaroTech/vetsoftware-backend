package com.vetsoftware.app.rolepermission.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.rolepermission.application.port.in.DeleteRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import com.vetsoftware.app.rolepermission.domain.RolePermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "rolepermission.delete")
@Service
public class DeleteRolePermissionService implements DeleteRolePermissionUseCase {
    private final RolePermissionRepository repository;

    public DeleteRolePermissionService(RolePermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(Long id, AuthContext auth) {
        repository.findById(id).orElseThrow(() -> new RolePermissionNotFoundException(id));
        repository.delete(id);
    }
}
