package com.vetsoftware.app.basepermission.application.usecase;

import com.vetsoftware.app.basepermission.application.port.in.DeleteBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.out.BasePermissionRepository;
import com.vetsoftware.app.basepermission.application.port.out.BaseRolePermissionChildrenQueryPort;
import com.vetsoftware.app.basepermission.domain.BasePermissionHasActiveChildrenException;
import com.vetsoftware.app.basepermission.domain.BasePermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "base.permission.delete")
@Service
public class DeleteBasePermissionService implements DeleteBasePermissionUseCase {
    private final BasePermissionRepository repository;
    private final BaseRolePermissionChildrenQueryPort baseRolePermissionChildrenQueryPort;

    public DeleteBasePermissionService(BasePermissionRepository repository,
            BaseRolePermissionChildrenQueryPort baseRolePermissionChildrenQueryPort) {
        this.repository = repository;
        this.baseRolePermissionChildrenQueryPort = baseRolePermissionChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new BasePermissionNotFoundException(id));
        if (baseRolePermissionChildrenQueryPort.existsActiveByBasePermissionId(id)) {
            throw new BasePermissionHasActiveChildrenException(id, "baseRolePermission");
        }
        repository.delete(id);
    }
}
