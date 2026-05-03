package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.registration.application.port.out.RoleCreator;
import com.vetsoftware.app.role.application.command.CreateRoleCommand;
import com.vetsoftware.app.role.application.port.in.CreateRoleUseCase;
import org.springframework.stereotype.Component;

@Component
public class CreateRoleAdapter implements RoleCreator {

    private final CreateRoleUseCase createRoleUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public CreateRoleAdapter(CreateRoleUseCase createRoleUseCase,
                             SystemAuthRunner systemAuthRunner) {
        this.createRoleUseCase = createRoleUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public RoleResult create(String name, String code, Long companyId) {
        var dto = systemAuthRunner.call(() -> createRoleUseCase.execute(
            new CreateRoleCommand(name, code, companyId)
        ));
        return new RoleResult(dto.id());
    }
}
