package com.vetsoftware.app.systemuserpermission.application.usecase;

import com.vetsoftware.app.systemuserpermission.application.port.in.DeleteSystemUserPermissionUseCase;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserPermissionRepository;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "system.user.permission.delete")
@Service
public class DeleteSystemUserPermissionService implements DeleteSystemUserPermissionUseCase {
    private final SystemUserPermissionRepository repository;

    public DeleteSystemUserPermissionService(SystemUserPermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new SystemUserPermissionNotFoundException(id));
        repository.delete(id);
    }
}
