package com.vetsoftware.app.registration.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.employeerole.application.command.CreateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.port.in.CreateEmployeeRoleUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateEmployeeRoleAdapter")
class CreateEmployeeRoleAdapterTest {

    @Mock
    private CreateEmployeeRoleUseCase createEmployeeRoleUseCase;
    @Mock
    private SystemAuthRunner systemAuthRunner;
    @InjectMocks
    private CreateEmployeeRoleAdapter adapter;

    @Test
    @DisplayName("asigna el rol al empleado dentro del contexto de sistema")
    void asigna_el_rol_bajo_contexto_de_sistema() {
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(systemAuthRunner).run(any(Runnable.class));

        adapter.assign(55L, 100L);

        ArgumentCaptor<CreateEmployeeRoleCommand> captor = ArgumentCaptor
                .forClass(CreateEmployeeRoleCommand.class);
        verify(createEmployeeRoleUseCase).execute(captor.capture());
        assertThat(captor.getValue().employeeId()).isEqualTo(55L);
        assertThat(captor.getValue().roleId()).isEqualTo(100L);
        // Camino SYSTEM declarado: el registro crea empleado y rol en la misma
        // transaccion, no hay principal con empresa al que preguntarle.
        assertThat(captor.getValue().companyId()).isNull();
    }

    @Test
    @DisplayName("la asignación ocurre solo dentro del system auth runner")
    void la_asignacion_ocurre_solo_dentro_del_system_auth_runner() {
        adapter.assign(55L, 100L);

        verify(createEmployeeRoleUseCase, never()).execute(any());
        verify(systemAuthRunner).run(any(Runnable.class));
    }
}
