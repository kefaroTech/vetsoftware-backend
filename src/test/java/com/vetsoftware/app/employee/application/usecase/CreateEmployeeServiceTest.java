package com.vetsoftware.app.employee.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.application.command.CreateEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.Employee;
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
 * Alta del dueño auto-registrado (Opción B): a diferencia de
 * {@code InviteEmployeeService}, nunca fuerza el cambio de contraseña — el
 * dueño elige la suya.
 */
@ExtendWith(MockitoExtension.class)
class CreateEmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private PasswordHasher passwordHasher;
    @InjectMocks
    private CreateEmployeeService service;

    @Nested
    class AltaCorrecta {

        @Test
        @DisplayName("persiste al empleado con la contrasena hasheada")
        void persiste_con_la_contrasena_hasheada() {
            when(companyQueryPort.findById(EmployeeMother.COMPANY_ID))
                    .thenReturn(Optional.of(EmployeeMother.VETRINA));
            when(passwordHasher.hash("Temporal123*")).thenReturn("$2a$10$hashed");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(EmployeeMother.comandoCrear(true));

            ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getHashPassword()).isEqualTo("$2a$10$hashed");
        }

        @Test
        @DisplayName("nunca fuerza el cambio de contrasena en el primer login")
        void nunca_fuerza_el_cambio_de_contrasena() {
            when(companyQueryPort.findById(EmployeeMother.COMPANY_ID))
                    .thenReturn(Optional.of(EmployeeMother.VETRINA));
            when(passwordHasher.hash(EmployeeMother.comandoCrear(true).password()))
                    .thenReturn("$2a$10$hashed");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(EmployeeMother.comandoCrear(true));

            ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().isMustChangePassword()).isFalse();
        }

        @Test
        @DisplayName("respeta el emailVerified recibido en el comando")
        void respeta_el_email_verified_recibido() {
            when(companyQueryPort.findById(EmployeeMother.COMPANY_ID))
                    .thenReturn(Optional.of(EmployeeMother.VETRINA));
            when(passwordHasher.hash(EmployeeMother.comandoCrear(false).password()))
                    .thenReturn("$2a$10$hashed");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EmployeeDto dto = service.execute(EmployeeMother.comandoCrear(false));

            assertThat(dto.company().id()).isEqualTo(EmployeeMother.COMPANY_ID);
        }
    }

    @Nested
    class Rechazos {

        @Test
        @DisplayName("una empresa inexistente aborta el alta antes de hashear")
        void una_empresa_inexistente_aborta_antes_de_hashear() {
            CreateEmployeeCommand command = EmployeeMother.comandoCrear(true);
            when(companyQueryPort.findById(command.companyId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found");

            verifyNoInteractions(passwordHasher);
            verify(repository, never()).save(any());
        }
    }
}
