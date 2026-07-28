package com.vetsoftware.app.baserole.application.usecase;

import com.vetsoftware.app.baserole.application.port.in.DeleteBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.out.BaseRolePermissionChildrenQueryPort;
import com.vetsoftware.app.baserole.application.port.out.BaseRoleRepository;
import com.vetsoftware.app.baserole.domain.BaseRoleHasActiveChildrenException;
import com.vetsoftware.app.baserole.domain.BaseRoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "base.role.delete")
@Service
public class DeleteBaseRoleService implements DeleteBaseRoleUseCase {
    private final BaseRoleRepository repository;
    private final BaseRolePermissionChildrenQueryPort baseRolePermissionChildrenQueryPort;

    public DeleteBaseRoleService(
            BaseRoleRepository repository,
            BaseRolePermissionChildrenQueryPort baseRolePermissionChildrenQueryPort) {
        this.repository = repository;
        this.baseRolePermissionChildrenQueryPort = baseRolePermissionChildrenQueryPort;
    }

    @Override
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new BaseRoleNotFoundException(id));
        if (baseRolePermissionChildrenQueryPort.existsActiveByBaseRoleId(id)) {
            throw new BaseRoleHasActiveChildrenException(id, "baseRolePermission");
        }
        repository.delete(id);
    }
}
