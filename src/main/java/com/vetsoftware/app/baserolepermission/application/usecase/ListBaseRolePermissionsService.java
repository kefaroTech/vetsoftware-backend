package com.vetsoftware.app.baserolepermission.application.usecase;

import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import com.vetsoftware.app.baserolepermission.application.port.in.ListBaseRolePermissionsUseCase;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRolePermissionRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "base.role.permission.list")
@Service
public class ListBaseRolePermissionsService implements ListBaseRolePermissionsUseCase {
    private final BaseRolePermissionRepository repository;

    public ListBaseRolePermissionsService(BaseRolePermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<BaseRolePermissionDto> listAll() {
        return repository.findAll().stream().map(BaseRolePermissionDto::from).toList();
    }
}
