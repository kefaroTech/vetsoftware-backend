package com.vetsoftware.app.basepermission.application.usecase;

import com.vetsoftware.app.basepermission.application.command.UpdateBasePermissionCommand;
import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import com.vetsoftware.app.basepermission.application.port.in.UpdateBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.out.BasePermissionRepository;
import com.vetsoftware.app.basepermission.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.basepermission.domain.BasePermission;
import com.vetsoftware.app.basepermission.domain.BasePermissionNotFoundException;
import com.vetsoftware.app.basepermission.domain.SubModuleRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "basepermission.update")
@Service
public class UpdateBasePermissionService implements UpdateBasePermissionUseCase {
    private final BasePermissionRepository repository;
    private final SubModuleQueryPort subModuleQueryPort;

    public UpdateBasePermissionService(BasePermissionRepository repository,
                                       SubModuleQueryPort subModuleQueryPort) {
        this.repository = repository;
        this.subModuleQueryPort = subModuleQueryPort;
    }

    @Override
    @Transactional
    public BasePermissionDto execute(UpdateBasePermissionCommand command) {
        BasePermission basePermission = repository.findById(command.id())
            .orElseThrow(() -> new BasePermissionNotFoundException(command.id()));
        SubModuleRef subModule = subModuleQueryPort.findById(command.subModuleId())
            .orElseThrow(() -> new IllegalArgumentException("SubModule not found: " + command.subModuleId()));
        basePermission.update(command.name(), command.code(), subModule);
        return BasePermissionDto.from(repository.save(basePermission));
    }
}
