package com.vetsoftware.app.baserole.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.baserole.application.command.CreateBaseRoleCommand;
import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import com.vetsoftware.app.baserole.application.port.in.CreateBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.out.BaseRoleRepository;
import com.vetsoftware.app.baserole.domain.BaseRole;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "baserole.create")
@Service
public class CreateBaseRoleService implements CreateBaseRoleUseCase {
    private final BaseRoleRepository repository;

    public CreateBaseRoleService(BaseRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public BaseRoleDto execute(CreateBaseRoleCommand command, AuthContext auth) {
        BaseRole baseRole = BaseRole.create(command.name(), command.code(), command.mandatory());
        return BaseRoleDto.from(repository.save(baseRole));
    }
}
