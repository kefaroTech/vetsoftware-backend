package com.vetsoftware.app.passwordreset.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.vetsoftware.app.employee.application.command.ResetEmployeePasswordCommand;
import com.vetsoftware.app.employee.application.port.in.ResetEmployeePasswordUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeePasswordResetterAdapter — orquesta el reseteo de la contrasena del empleado")
class EmployeePasswordResetterAdapterTest {

    @Mock
    private ResetEmployeePasswordUseCase resetEmployeePasswordUseCase;

    @InjectMocks
    private EmployeePasswordResetterAdapter adapter;

    @Test
    @DisplayName("traduce (employeeId, password) al command del caso de uso de employee")
    void traduce_al_command_del_caso_de_uso_de_employee() {
        adapter.reset(500L, "nuevaClave123");

        ArgumentCaptor<ResetEmployeePasswordCommand> captor = ArgumentCaptor
                .forClass(ResetEmployeePasswordCommand.class);
        verify(resetEmployeePasswordUseCase).execute(captor.capture());
        assertThat(captor.getValue().employeeId()).isEqualTo(500L);
        assertThat(captor.getValue().newPassword()).isEqualTo("nuevaClave123");
    }
}
