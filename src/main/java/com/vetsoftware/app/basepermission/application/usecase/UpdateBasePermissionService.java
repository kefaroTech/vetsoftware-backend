package com.vetsoftware.app.basepermission.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.basepermission.application.command.UpdateBasePermissionCommand;
import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import com.vetsoftware.app.basepermission.application.port.in.UpdateBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.out.BasePermissionRepository;
import com.vetsoftware.app.basepermission.application.port.out.SubModuleValidationPort;
import com.vetsoftware.app.basepermission.domain.BasePermission;
import com.vetsoftware.app.basepermission.domain.BasePermissionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "basepermission.update")
@Service
public class UpdateBasePermissionService implements UpdateBasePermissionUseCase {
    private final BasePermissionRepository repository;
    private final SubModuleValidationPort subModuleValidationPort;

    public UpdateBasePermissionService(BasePermissionRepository repository,
                                       SubModuleValidationPort subModuleValidationPort) {
        this.repository = repository;
        this.subModuleValidationPort = subModuleValidationPort;
    }

    @Override
    @Transactional
    public BasePermissionDto execute(UpdateBasePermissionCommand command, AuthContext auth) {
        BasePermission basePermission = repository.findById(command.id())
            .orElseThrow(() -> new BasePermissionNotFoundException(command.id()));
        subModuleValidationPort.validateExists(command.subModuleId());
        basePermission.update(command.name(), command.code(), command.subModuleId());
        return BasePermissionDto.from(repository.save(basePermission));
    }
}
