package com.vetsoftware.app.registration.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.employee.application.command.CreateEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.CompanySummaryDto;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.in.CreateEmployeeUseCase;
import com.vetsoftware.app.registration.application.port.out.EmployeeCreator.EmployeeResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El dueño se crea SIN verificar (Opción B: no puede iniciar sesión hasta
 * confirmar el correo) y con la contraseña CRUDA — el hasheo lo hace
 * {@code CreateEmployeeService} una sola vez; pre-hashear aquí la doblaría.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateEmployeeAdapter")
class CreateEmployeeAdapterTest {

    @Mock
    private CreateEmployeeUseCase createEmployeeUseCase;
    @Mock
    private SystemAuthRunner systemAuthRunner;
    @InjectMocks
    private CreateEmployeeAdapter adapter;

    @BeforeEach
    void setUp() {
        when(systemAuthRunner.call(any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
    }

    private static EmployeeDto dto() {
        return new EmployeeDto(55L, "orlando@vetrina.co", "Orlando Velásquez", "orlando@vetrina.co",
                new CompanySummaryDto(9L, "Veterinaria Vetrina", "900123456"), List.of(), List.of(),
                LocalDateTime.of(2026, 1, 15, 10, 30), true, false, "ACTIVE");
    }

    @Test
    @DisplayName("crea al dueño sin verificar, con la contraseña cruda para que el service la hashee")
    void crea_al_dueno_sin_verificar_con_password_cruda() {
        when(createEmployeeUseCase.execute(any(CreateEmployeeCommand.class))).thenReturn(dto());

        EmployeeResult result = adapter.create("orlando@vetrina.co", "Orlando1997*",
                "Orlando Velásquez", "orlando@vetrina.co", 9L);

        ArgumentCaptor<CreateEmployeeCommand> captor = ArgumentCaptor
                .forClass(CreateEmployeeCommand.class);
        verify(createEmployeeUseCase).execute(captor.capture());
        CreateEmployeeCommand command = captor.getValue();
        assertThat(command.employeeCode()).isEqualTo("orlando@vetrina.co");
        assertThat(command.password()).isEqualTo("Orlando1997*");
        assertThat(command.name()).isEqualTo("Orlando Velásquez");
        assertThat(command.email()).isEqualTo("orlando@vetrina.co");
        assertThat(command.companyId()).isEqualTo(9L);
        assertThat(command.emailVerified()).as("el dueño auto-registrado arranca sin verificar")
                .isFalse();
        assertThat(result.id()).isEqualTo(55L);
    }

    @Test
    @DisplayName("entrega la contraseña sin transformar: el hasheo es responsabilidad del service")
    void entrega_la_contrasena_sin_transformar() {
        when(createEmployeeUseCase.execute(any(CreateEmployeeCommand.class))).thenReturn(dto());

        adapter.create("orlando@vetrina.co", "Orlando1997*", "Orlando", "orlando@vetrina.co", 9L);

        verify(createEmployeeUseCase).execute(
                org.mockito.ArgumentMatchers.argThat(cmd -> "Orlando1997*".equals(cmd.password())));
    }
}
