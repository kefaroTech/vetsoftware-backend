package com.vetsoftware.app.systemuser.application.usecase;

import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import com.vetsoftware.app.systemuser.application.port.in.ListSystemUsersUseCase;
import com.vetsoftware.app.systemuser.application.port.out.SystemUserRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "systemuser.list")
@Service
public class ListSystemUsersService implements ListSystemUsersUseCase {
    private final SystemUserRepository repository;

    public ListSystemUsersService(SystemUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SystemUserDto> listAll() {
        return repository.findAll().stream().map(SystemUserDto::from).toList();
    }
}
