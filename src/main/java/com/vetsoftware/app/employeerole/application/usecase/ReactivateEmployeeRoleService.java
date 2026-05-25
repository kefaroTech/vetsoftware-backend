package com.vetsoftware.app.employeerole.application.usecase;

import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import com.vetsoftware.app.employeerole.application.port.in.ReactivateEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeRoleRepository;
import com.vetsoftware.app.employeerole.domain.EmployeeRoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "employeerole.reactivate")
@Service
public class ReactivateEmployeeRoleService implements ReactivateEmployeeRoleUseCase {
    private final EmployeeRoleRepository repository;

    public ReactivateEmployeeRoleService(EmployeeRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public EmployeeRoleDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new EmployeeRoleNotFoundException(id);
        return EmployeeRoleDto.from(repository.findById(id)
            .orElseThrow(() -> new EmployeeRoleNotFoundException(id)));
    }
}
