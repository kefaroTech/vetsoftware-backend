package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.employeerole.application.command.CreateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.port.in.CreateEmployeeRoleUseCase;
import com.vetsoftware.app.registration.application.port.out.EmployeeRoleAssigner;
import org.springframework.stereotype.Component;

@Component
public class CreateEmployeeRoleAdapter implements EmployeeRoleAssigner {

    private final CreateEmployeeRoleUseCase createEmployeeRoleUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public CreateEmployeeRoleAdapter(CreateEmployeeRoleUseCase createEmployeeRoleUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.createEmployeeRoleUseCase = createEmployeeRoleUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    /**
     * Camino SYSTEM declarado: {@code companyId} nulo. El registro se auto-asigna
     * el rol ADMIN de la empresa que acaba de crear, y lo hace bajo
     * {@link SystemAuthRunner} porque todavia no existe ningun empleado con sesion
     * al que preguntarle su empresa. El empleado y el rol los acaba de generar el
     * propio {@code RegisterUserService} en la misma transaccion —no vienen del
     * cliente—, asi que aqui no hay id que adivinar y la resolucion ancha es la
     * correcta.
     */
    @Override
    public void assign(Long employeeId, Long roleId) {
        systemAuthRunner.run(() -> createEmployeeRoleUseCase
                .execute(new CreateEmployeeRoleCommand(employeeId, roleId, null)));
    }
}
