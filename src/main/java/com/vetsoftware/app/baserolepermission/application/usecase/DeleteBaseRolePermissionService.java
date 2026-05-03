package com.vetsoftware.app.baserolepermission.application.usecase;

import com.vetsoftware.app.baserolepermission.application.port.in.DeleteBaseRolePermissionUseCase;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRolePermissionRepository;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "baserolepermission.delete")
@Service
public class DeleteBaseRolePermissionService implements DeleteBaseRolePermissionUseCase {
    private final BaseRolePermissionRepository repository;

    public DeleteBaseRolePermissionService(BaseRolePermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new BaseRolePermissionNotFoundException(id));
        repository.delete(id);
    }
}
