package com.vetsoftware.app.owner.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.owner.application.dto.OwnerDto;
import com.vetsoftware.app.owner.application.port.in.ListOwnersUseCase;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "owner.list")
@Service
public class ListOwnersService implements ListOwnersUseCase {
    private final OwnerRepository repository;

    public ListOwnersService(OwnerRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<OwnerDto> listAll(AuthContext auth) {
        return repository.findAll().stream().map(OwnerDto::from).toList();
    }
}
