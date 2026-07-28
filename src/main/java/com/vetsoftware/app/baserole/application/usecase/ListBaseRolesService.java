package com.vetsoftware.app.baserole.application.usecase;

import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import com.vetsoftware.app.baserole.application.port.in.ListBaseRolesUseCase;
import com.vetsoftware.app.baserole.application.port.out.BaseRoleRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "base.role.list")
@Service
public class ListBaseRolesService implements ListBaseRolesUseCase {
    private final BaseRoleRepository repository;

    public ListBaseRolesService(BaseRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<BaseRoleDto> listAll() {
        return repository.findAll().stream().map(BaseRoleDto::from).toList();
    }
}
