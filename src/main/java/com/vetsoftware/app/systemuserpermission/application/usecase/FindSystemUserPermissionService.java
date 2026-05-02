package com.vetsoftware.app.systemuserpermission.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import com.vetsoftware.app.systemuserpermission.application.port.in.FindSystemUserPermissionUseCase;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserPermissionRepository;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "systemuserpermission.find")
@Service
public class FindSystemUserPermissionService implements FindSystemUserPermissionUseCase {
    private final SystemUserPermissionRepository repository;

    public FindSystemUserPermissionService(SystemUserPermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public SystemUserPermissionDto findById(Long id, AuthContext auth) {
        return repository.findById(id)
            .map(SystemUserPermissionDto::from)
            .orElseThrow(() -> new SystemUserPermissionNotFoundException(id));
    }
}
