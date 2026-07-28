package com.vetsoftware.app.baserolepermission.application.usecase;

import com.vetsoftware.app.baserolepermission.application.command.CreateBaseRolePermissionCommand;
import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import com.vetsoftware.app.baserolepermission.application.port.in.CreateBaseRolePermissionUseCase;
import com.vetsoftware.app.baserolepermission.application.port.out.BasePermissionQueryPort;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRolePermissionRepository;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRoleQueryPort;
import com.vetsoftware.app.baserolepermission.domain.BasePermissionRef;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermission;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermissionNotFoundException;
import com.vetsoftware.app.baserolepermission.domain.BaseRoleRef;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "base.role.permission.create")
@Service
public class CreateBaseRolePermissionService implements CreateBaseRolePermissionUseCase {
    private final BaseRolePermissionRepository repository;
    private final BaseRoleQueryPort baseRoleQueryPort;
    private final BasePermissionQueryPort basePermissionQueryPort;

    public CreateBaseRolePermissionService(BaseRolePermissionRepository repository,
                                            BaseRoleQueryPort baseRoleQueryPort,
                                            BasePermissionQueryPort basePermissionQueryPort) {
        this.repository = repository;
        this.baseRoleQueryPort = baseRoleQueryPort;
        this.basePermissionQueryPort = basePermissionQueryPort;
    }

    @Override
    @Transactional
    public BaseRolePermissionDto execute(CreateBaseRolePermissionCommand command) {
        BaseRoleRef baseRole = baseRoleQueryPort.findById(command.baseRoleId())
            .orElseThrow(() -> new IllegalArgumentException("BaseRole not found: " + command.baseRoleId()));
        BasePermissionRef basePermission = basePermissionQueryPort.findById(command.basePermissionId())
            .orElseThrow(() -> new IllegalArgumentException("BasePermission not found: " + command.basePermissionId()));

        Optional<Long> disabledId = repository
            .findDisabledIdByBaseRoleAndBasePermission(command.baseRoleId(), command.basePermissionId());
        if (disabledId.isPresent()) {
            Long id = disabledId.get();
            repository.reactivate(id);
            BaseRolePermission refreshed = repository.findById(id)
                .orElseThrow(() -> new BaseRolePermissionNotFoundException(id));
            return BaseRolePermissionDto.from(refreshed);
        }

        BaseRolePermission baseRolePermission = BaseRolePermission.create(baseRole, basePermission);
        return BaseRolePermissionDto.from(repository.save(baseRolePermission));
    }
}
