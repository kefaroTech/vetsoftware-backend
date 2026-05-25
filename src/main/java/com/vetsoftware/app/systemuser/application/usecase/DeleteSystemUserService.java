package com.vetsoftware.app.systemuser.application.usecase;

import com.vetsoftware.app.systemuser.application.port.in.DeleteSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.out.SystemUserPermissionChildrenQueryPort;
import com.vetsoftware.app.systemuser.application.port.out.SystemUserRepository;
import com.vetsoftware.app.systemuser.domain.SystemUserHasActiveChildrenException;
import com.vetsoftware.app.systemuser.domain.SystemUserNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "systemuser.delete")
@Service
public class DeleteSystemUserService implements DeleteSystemUserUseCase {
    private final SystemUserRepository repository;
    private final SystemUserPermissionChildrenQueryPort systemUserPermissionChildrenQueryPort;

    public DeleteSystemUserService(
            SystemUserRepository repository,
            SystemUserPermissionChildrenQueryPort systemUserPermissionChildrenQueryPort) {
        this.repository = repository;
        this.systemUserPermissionChildrenQueryPort = systemUserPermissionChildrenQueryPort;
    }

    @Override
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new SystemUserNotFoundException(id));
        if (systemUserPermissionChildrenQueryPort.existsActiveBySystemUserId(id)) {
            throw new SystemUserHasActiveChildrenException(id, "systemUserPermission");
        }
        repository.delete(id);
    }
}
