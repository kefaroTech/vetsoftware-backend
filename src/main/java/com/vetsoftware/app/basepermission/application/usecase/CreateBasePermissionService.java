package com.vetsoftware.app.basepermission.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.basepermission.application.command.CreateBasePermissionCommand;
import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import com.vetsoftware.app.basepermission.application.port.in.CreateBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.out.BasePermissionRepository;
import com.vetsoftware.app.basepermission.application.port.out.MandatoryBaseRolePermissionInitializationPort;
import com.vetsoftware.app.basepermission.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.basepermission.domain.BasePermission;
import com.vetsoftware.app.basepermission.domain.SubModuleRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "basepermission.create")
@Service
public class CreateBasePermissionService implements CreateBasePermissionUseCase {
    private final BasePermissionRepository repository;
    private final SubModuleQueryPort subModuleQueryPort;
    private final MandatoryBaseRolePermissionInitializationPort mandatoryBaseRolePermissionInitializationPort;

    public CreateBasePermissionService(BasePermissionRepository repository,
                                       SubModuleQueryPort subModuleQueryPort,
                                       MandatoryBaseRolePermissionInitializationPort mandatoryBaseRolePermissionInitializationPort) {
        this.repository = repository;
        this.subModuleQueryPort = subModuleQueryPort;
        this.mandatoryBaseRolePermissionInitializationPort = mandatoryBaseRolePermissionInitializationPort;
    }

    @Override
    @Transactional
    public BasePermissionDto execute(CreateBasePermissionCommand command, AuthContext auth) {
        SubModuleRef subModule = subModuleQueryPort.findById(command.subModuleId())
            .orElseThrow(() -> new IllegalArgumentException("SubModule not found: " + command.subModuleId()));
        BasePermission basePermission = BasePermission.create(command.name(), command.code(), subModule);
        BasePermissionDto saved = BasePermissionDto.from(repository.save(basePermission));
        mandatoryBaseRolePermissionInitializationPort.initializeForMandatoryBaseRoles(saved.id());
        return saved;
    }
}
