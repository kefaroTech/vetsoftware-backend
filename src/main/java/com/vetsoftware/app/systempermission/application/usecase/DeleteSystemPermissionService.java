package com.vetsoftware.app.systempermission.application.usecase;

import com.vetsoftware.app.systempermission.application.port.in.DeleteSystemPermissionUseCase;
import com.vetsoftware.app.systempermission.application.port.out.SystemPermissionRepository;
import com.vetsoftware.app.systempermission.domain.SystemPermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "systempermission.delete")
@Service
public class DeleteSystemPermissionService implements DeleteSystemPermissionUseCase {
    private final SystemPermissionRepository repository;

    public DeleteSystemPermissionService(SystemPermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new SystemPermissionNotFoundException(id));
        repository.delete(id);
    }
}
