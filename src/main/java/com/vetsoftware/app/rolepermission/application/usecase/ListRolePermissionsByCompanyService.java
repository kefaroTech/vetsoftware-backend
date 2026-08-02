package com.vetsoftware.app.rolepermission.application.usecase;

import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.port.in.ListRolePermissionsByCompanyUseCase;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "role.permission.list.by.company")
@Service
public class ListRolePermissionsByCompanyService implements ListRolePermissionsByCompanyUseCase {
    private final RolePermissionRepository repository;

    public ListRolePermissionsByCompanyService(RolePermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RolePermissionDto> listByCompany(Long companyId) {
        return repository.findAllByRoleCompanyId(companyId).stream().map(RolePermissionDto::from)
                .toList();
    }
}
