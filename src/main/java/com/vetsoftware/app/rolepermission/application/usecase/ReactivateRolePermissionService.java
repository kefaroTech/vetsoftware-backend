package com.vetsoftware.app.rolepermission.application.usecase;

import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.port.in.ReactivateRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import com.vetsoftware.app.rolepermission.domain.RolePermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "rolepermission.reactivate")
@Service
public class ReactivateRolePermissionService implements ReactivateRolePermissionUseCase {
    private final RolePermissionRepository repository;

    public ReactivateRolePermissionService(RolePermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public RolePermissionDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new RolePermissionNotFoundException(id);
        return RolePermissionDto.from(repository.findById(id)
            .orElseThrow(() -> new RolePermissionNotFoundException(id)));
    }
}
