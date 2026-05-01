package com.vetsoftware.app.baserole.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.baserole.application.port.in.DeleteBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.out.BaseRoleRepository;
import com.vetsoftware.app.baserole.domain.BaseRoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "baserole.delete")
@Service
public class DeleteBaseRoleService implements DeleteBaseRoleUseCase {
    private final BaseRoleRepository repository;

    public DeleteBaseRoleService(BaseRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(Long id, AuthContext auth) {
        repository.findById(id).orElseThrow(() -> new BaseRoleNotFoundException(id));
        repository.delete(id);
    }
}
