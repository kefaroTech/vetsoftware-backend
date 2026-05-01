package com.vetsoftware.app.permission.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.permission.application.port.in.DeletePermissionUseCase;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.domain.PermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "permission.delete")
@Service
public class DeletePermissionService implements DeletePermissionUseCase {
    private final PermissionRepository repository;

    public DeletePermissionService(PermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(Long id, AuthContext auth) {
        repository.findById(id).orElseThrow(() -> new PermissionNotFoundException(id));
        repository.delete(id);
    }
}
