package com.vetsoftware.app.systemuser.application.usecase;

import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import com.vetsoftware.app.systemuser.application.port.in.FindSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.out.SystemUserRepository;
import com.vetsoftware.app.systemuser.domain.SystemUserNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "system.user.find")
@Service
public class FindSystemUserService implements FindSystemUserUseCase {
    private final SystemUserRepository repository;

    public FindSystemUserService(SystemUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public SystemUserDto findById(Long id) {
        return repository.findById(id).map(SystemUserDto::from)
                .orElseThrow(() -> new SystemUserNotFoundException(id));
    }
}
