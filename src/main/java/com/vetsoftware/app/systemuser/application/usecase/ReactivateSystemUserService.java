package com.vetsoftware.app.systemuser.application.usecase;

import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import com.vetsoftware.app.systemuser.application.port.in.ReactivateSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.out.SystemUserRepository;
import com.vetsoftware.app.systemuser.domain.SystemUserNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "system.user.reactivate")
@Service
public class ReactivateSystemUserService implements ReactivateSystemUserUseCase {
    private final SystemUserRepository repository;

    public ReactivateSystemUserService(SystemUserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SystemUserDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new SystemUserNotFoundException(id);
        return SystemUserDto.from(repository.findById(id)
            .orElseThrow(() -> new SystemUserNotFoundException(id)));
    }
}
