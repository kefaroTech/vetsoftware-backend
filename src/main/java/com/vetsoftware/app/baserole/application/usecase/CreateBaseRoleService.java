package com.vetsoftware.app.baserole.application.usecase;

import com.vetsoftware.app.baserole.application.command.CreateBaseRoleCommand;
import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import com.vetsoftware.app.baserole.application.port.in.CreateBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.out.BaseRolePermissionInitializationPort;
import com.vetsoftware.app.baserole.application.port.out.BaseRoleRepository;
import com.vetsoftware.app.baserole.domain.BaseRole;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "base.role.create")
@Service
public class CreateBaseRoleService implements CreateBaseRoleUseCase {
    private final BaseRoleRepository repository;
    private final BaseRolePermissionInitializationPort baseRolePermissionInitializationPort;

    public CreateBaseRoleService(BaseRoleRepository repository, BaseRolePermissionInitializationPort baseRolePermissionInitializationPort) {
        this.repository = repository;
        this.baseRolePermissionInitializationPort = baseRolePermissionInitializationPort;
    }

    @Override
    @Transactional
    public BaseRoleDto execute(CreateBaseRoleCommand command) {
        BaseRole baseRole = BaseRole.create(command.name(), command.code(), command.mandatory());
        BaseRoleDto baseRoleDtoResponse = BaseRoleDto.from(repository.save(baseRole));
        if(baseRole.getMandatory()){
            baseRolePermissionInitializationPort.initializeForAllBasePermissions(baseRoleDtoResponse.id());
        }
        return baseRoleDtoResponse;
    }
}
