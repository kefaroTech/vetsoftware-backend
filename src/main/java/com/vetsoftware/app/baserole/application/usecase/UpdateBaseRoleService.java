package com.vetsoftware.app.baserole.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.baserole.application.command.UpdateBaseRoleCommand;
import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import com.vetsoftware.app.baserole.application.port.in.UpdateBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.out.BaseRoleRepository;
import com.vetsoftware.app.baserole.domain.BaseRole;
import com.vetsoftware.app.baserole.domain.BaseRoleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "baserole.update")
@Service
public class UpdateBaseRoleService implements UpdateBaseRoleUseCase {
    private final BaseRoleRepository repository;

    public UpdateBaseRoleService(BaseRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public BaseRoleDto execute(UpdateBaseRoleCommand command, AuthContext auth) {
        BaseRole baseRole = repository.findById(command.id())
            .orElseThrow(() -> new BaseRoleNotFoundException(command.id()));
        baseRole.update(command.name(), command.code(), command.mandatory());
        return BaseRoleDto.from(repository.save(baseRole));
    }
}
