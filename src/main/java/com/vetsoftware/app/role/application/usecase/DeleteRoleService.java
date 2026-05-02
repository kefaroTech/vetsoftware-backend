package com.vetsoftware.app.role.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.role.application.port.in.DeleteRoleUseCase;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.domain.RoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "role.delete")
@Service
public class DeleteRoleService implements DeleteRoleUseCase {
    private final RoleRepository repository;

    public DeleteRoleService(RoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(Long id, AuthContext auth) {
        repository.findById(id).orElseThrow(() -> new RoleNotFoundException(id));
        repository.delete(id);
    }
}
