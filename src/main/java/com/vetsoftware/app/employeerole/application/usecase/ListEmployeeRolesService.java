package com.vetsoftware.app.employeerole.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import com.vetsoftware.app.employeerole.application.port.in.ListEmployeeRolesUseCase;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeRoleRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "employeerole.list")
@Service
public class ListEmployeeRolesService implements ListEmployeeRolesUseCase {
    private final EmployeeRoleRepository repository;

    public ListEmployeeRolesService(EmployeeRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<EmployeeRoleDto> listAll(AuthContext auth) {
        return repository.findAll().stream().map(EmployeeRoleDto::from).toList();
    }
}
