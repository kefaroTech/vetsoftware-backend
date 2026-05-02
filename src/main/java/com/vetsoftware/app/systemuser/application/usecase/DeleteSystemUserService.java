package com.vetsoftware.app.systemuser.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.systemuser.application.port.in.DeleteSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.out.SystemUserRepository;
import com.vetsoftware.app.systemuser.domain.SystemUserNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "systemuser.delete")
@Service
public class DeleteSystemUserService implements DeleteSystemUserUseCase {
    private final SystemUserRepository repository;

    public DeleteSystemUserService(SystemUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(Long id, AuthContext auth) {
        repository.findById(id).orElseThrow(() -> new SystemUserNotFoundException(id));
        repository.delete(id);
    }
}
