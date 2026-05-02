package com.vetsoftware.app.rolepermission.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.port.in.ListRolePermissionsUseCase;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "rolepermission.list")
@Service
public class ListRolePermissionsService implements ListRolePermissionsUseCase {
    private final RolePermissionRepository repository;

    public ListRolePermissionsService(RolePermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RolePermissionDto> listAll(AuthContext auth) {
        return repository.findAll().stream().map(RolePermissionDto::from).toList();
    }
}
