package com.vetsoftware.app.permission.application.usecase;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.port.in.ListPermissionsByCompanyUseCase;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "permission.list.by.company")
@Service
public class ListPermissionsByCompanyService implements ListPermissionsByCompanyUseCase {
    private final PermissionRepository repository;

    public ListPermissionsByCompanyService(PermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PermissionDto> listByCompany(Long companyId) {
        return repository.findAllByCompanyId(companyId).stream().map(PermissionDto::from).toList();
    }
}
