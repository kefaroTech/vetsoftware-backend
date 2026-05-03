package com.vetsoftware.app.rolepermission.application.usecase;

import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.port.in.FindRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import com.vetsoftware.app.rolepermission.domain.RolePermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "rolepermission.find")
@Service
public class FindRolePermissionService implements FindRolePermissionUseCase {
    private final RolePermissionRepository repository;

    public FindRolePermissionService(RolePermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public RolePermissionDto findById(Long id) {
        return repository.findById(id)
            .map(RolePermissionDto::from)
            .orElseThrow(() -> new RolePermissionNotFoundException(id));
    }
}
