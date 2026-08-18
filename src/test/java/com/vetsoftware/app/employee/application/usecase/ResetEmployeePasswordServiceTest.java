package com.vetsoftware.app.employee.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.application.command.ResetEmployeePasswordCommand;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import com.vetsoftware.app.employee.testsupport.EmployeeMother;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Restablecimiento de contraseña ("olvidé mi contraseña"): a diferencia de un
 * cambio voluntario, invalida las sesiones vivas subiendo authVersion. Busca
 * incluyendo desactivados: restablecer no debe fallar solo porque el empleado
 * esté deshabilitado.
 */
@ExtendWith(MockitoExtension.class)
class ResetEmployeePasswordServiceTest {

    @Mock
    private EmployeeRepository repository;
    @Mock
    private PasswordHasher passwordHasher;
    @InjectMocks
    private ResetEmployeePasswordService service;

    @Nested
    class RestablecimientoCorrecto {

        @Test
        @DisplayName("hashea la nueva clave e invalida las sesiones vivas")
        void hashea_la_nueva_clave_e_invalida_sesiones() {
            Employee empleado = EmployeeMother.activo();
            when(repository.findByIdIncludingDisabled(EmployeeMother.EMPLOYEE_ID))
                    .thenReturn(Optional.of(empleado));
            when(passwordHasher.hash("Nueva123*")).thenReturn("$2a$10$reset");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(
                    new ResetEmployeePasswordCommand(EmployeeMother.EMPLOYEE_ID, "Nueva123*"));

            ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getHashPassword()).isEqualTo("$2a$10$reset");
            assertThat(captor.getValue().isMustChangePassword()).isFalse();
            assertThat(captor.getValue().getAuthVersion()).isEqualTo(1L);
        }
    }

    @Nested
    class Rechazos {

        @Test
        @DisplayName("un empleado inexistente no escribe nada")
        void un_empleado_inexistente_no_escribe_nada() {
            when(repository.findByIdIncludingDisabled(EmployeeMother.EMPLOYEE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(
                    new ResetEmployeePasswordCommand(EmployeeMother.EMPLOYEE_ID, "Nueva123*")))
                    .isInstanceOf(EmployeeNotFoundException.class);

            verify(repository, never()).save(any());
            verifyNoInteractions(passwordHasher);
        }
    }
}
