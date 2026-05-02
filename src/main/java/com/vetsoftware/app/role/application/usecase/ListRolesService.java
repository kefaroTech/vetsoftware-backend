package com.vetsoftware.app.role.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.in.ListRolesUseCase;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "role.list")
@Service
public class ListRolesService implements ListRolesUseCase {
    private final RoleRepository repository;

    public ListRolesService(RoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RoleDto> listAll(AuthContext auth) {
        return repository.findAll().stream().map(RoleDto::from).toList();
    }
}
