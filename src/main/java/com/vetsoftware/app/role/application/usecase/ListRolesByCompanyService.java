package com.vetsoftware.app.role.application.usecase;

import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.in.ListRolesByCompanyUseCase;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "role.list_by_company")
@Service
public class ListRolesByCompanyService implements ListRolesByCompanyUseCase {
    private final RoleRepository repository;

    public ListRolesByCompanyService(RoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RoleDto> listByCompany(Long companyId) {
        return repository.findAllByCompanyId(companyId).stream().map(RoleDto::from).toList();
    }
}
