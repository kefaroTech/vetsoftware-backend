package com.vetsoftware.app.systemuser.application.usecase;

import com.vetsoftware.app.systemuser.application.command.UpdateSystemUserCommand;
import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import com.vetsoftware.app.systemuser.application.port.in.UpdateSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.out.SystemUserRepository;
import com.vetsoftware.app.systemuser.domain.SystemUser;
import com.vetsoftware.app.systemuser.domain.SystemUserNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "system.user.update")
@Service
public class UpdateSystemUserService implements UpdateSystemUserUseCase {
    private final SystemUserRepository repository;

    public UpdateSystemUserService(SystemUserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SystemUserDto execute(UpdateSystemUserCommand command) {
        SystemUser systemUser = repository.findById(command.id())
            .orElseThrow(() -> new SystemUserNotFoundException(command.id()));
        systemUser.update(command.code());
        return SystemUserDto.from(repository.save(systemUser));
    }
}
