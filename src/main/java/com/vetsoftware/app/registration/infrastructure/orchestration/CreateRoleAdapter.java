package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.registration.application.port.out.RoleCreator;
import com.vetsoftware.app.role.application.command.CreateRoleCommand;
import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.in.CreateRoleUseCase;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CreateRoleAdapter implements RoleCreator {

    private static final AuthContext SYSTEM_CONTEXT =
        new AuthContext(null, null, Set.of("admin.all"));

    private final CreateRoleUseCase createRoleUseCase;

    public CreateRoleAdapter(CreateRoleUseCase createRoleUseCase) {
        this.createRoleUseCase = createRoleUseCase;
    }

    @Override
    public RoleResult create(String name, String code, Long companyId) {
        RoleDto dto = createRoleUseCase.execute(
            new CreateRoleCommand(name, code, companyId),
            SYSTEM_CONTEXT
        );
        return new RoleResult(dto.id());
    }
}
