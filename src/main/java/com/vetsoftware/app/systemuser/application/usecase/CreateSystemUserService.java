package com.vetsoftware.app.systemuser.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import com.vetsoftware.app.systemuser.application.command.CreateSystemUserCommand;
import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import com.vetsoftware.app.systemuser.application.port.in.CreateSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.out.SystemUserRepository;
import com.vetsoftware.app.systemuser.domain.SystemUser;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "systemuser.create")
@Service
public class CreateSystemUserService implements CreateSystemUserUseCase {
    private final SystemUserRepository repository;
    private final PasswordHasher passwordHasher;

    public CreateSystemUserService(SystemUserRepository repository, PasswordHasher passwordHasher) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public SystemUserDto execute(CreateSystemUserCommand command, AuthContext auth) {
        String hashed = passwordHasher.hash(command.password());
        SystemUser systemUser = SystemUser.create(command.code(), hashed);
        return SystemUserDto.from(repository.save(systemUser));
    }
}
