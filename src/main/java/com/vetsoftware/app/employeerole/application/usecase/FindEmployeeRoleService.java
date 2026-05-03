package com.vetsoftware.app.employeerole.application.usecase;

import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import com.vetsoftware.app.employeerole.application.port.in.FindEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeRoleRepository;
import com.vetsoftware.app.employeerole.domain.EmployeeRoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "employeerole.find")
@Service
public class FindEmployeeRoleService implements FindEmployeeRoleUseCase {
    private final EmployeeRoleRepository repository;

    public FindEmployeeRoleService(EmployeeRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public EmployeeRoleDto findById(Long id) {
        return repository.findById(id)
            .map(EmployeeRoleDto::from)
            .orElseThrow(() -> new EmployeeRoleNotFoundException(id));
    }
}
