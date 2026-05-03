package com.vetsoftware.app.employeerole.application.usecase;

import com.vetsoftware.app.employeerole.application.port.in.DeleteEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeRoleRepository;
import com.vetsoftware.app.employeerole.domain.EmployeeRoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "employeerole.delete")
@Service
public class DeleteEmployeeRoleService implements DeleteEmployeeRoleUseCase {
    private final EmployeeRoleRepository repository;

    public DeleteEmployeeRoleService(EmployeeRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new EmployeeRoleNotFoundException(id));
        repository.delete(id);
    }
}
