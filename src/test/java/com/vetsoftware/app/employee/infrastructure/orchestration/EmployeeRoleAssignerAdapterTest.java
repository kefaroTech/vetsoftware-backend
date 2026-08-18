package com.vetsoftware.app.employee.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employeerole.application.command.CreateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import com.vetsoftware.app.employeerole.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.employeerole.application.dto.RoleSummaryDto;
import com.vetsoftware.app.employeerole.application.port.in.CreateEmployeeRoleUseCase;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Adaptador de orquestación: delega en la feature {@code employeerole}. */
@ExtendWith(MockitoExtension.class)
class EmployeeRoleAssignerAdapterTest {

    @Mock
    private CreateEmployeeRoleUseCase createEmployeeRoleUseCase;
    @InjectMocks
    private EmployeeRoleAssignerAdapter adapter;

    /**
     * La empresa que recibe el adapter viaja dentro del command: es con lo que la
     * feature {@code employeerole} acota la resolucion del empleado y del rol.
     */
    @Test
    @DisplayName("asigna el rol con la empresa del alta y devuelve su nombre")
    void asigna_el_rol_y_devuelve_su_nombre() {
        EmployeeRoleDto dto = new EmployeeRoleDto(700L,
                new EmployeeSummaryDto(55L, "VV-MARIANA", "Mariana"),
                new RoleSummaryDto(3L, "Veterinario", "VET"), LocalDateTime.of(2026, 1, 15, 10, 30),
                true);
        when(createEmployeeRoleUseCase.execute(new CreateEmployeeRoleCommand(55L, 3L, 9L)))
                .thenReturn(dto);

        String nombreRol = adapter.assign(55L, 9L, 3L);

        assertThat(nombreRol).isEqualTo("Veterinario");
        verify(createEmployeeRoleUseCase).execute(new CreateEmployeeRoleCommand(55L, 3L, 9L));
    }
}
