package com.vetsoftware.app.employee.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import com.vetsoftware.app.employee.domain.EmployeeStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerifyEmployeeEmailServiceTest {

    private static final Long EMPLOYEE_ID = 55L;

    @Mock
    private EmployeeRepository repository;
    @InjectMocks
    private VerifyEmployeeEmailService service;

    private static Employee sinVerificar() {
        return new Employee(EMPLOYEE_ID, "VV-ORLANDO", "$2a$10$hash", "Orlando",
                "orlando@vetrina.co",
                new com.vetsoftware.app.employee.domain.CompanyRef(9L, "Veterinaria Vetrina",
                        "900123456"),
                LocalDateTime.of(2026, 1, 15, 10, 30), null, true, false, false,
                EmployeeStatus.ACTIVE, 0L);
    }

    @Nested
    class VerificacionCorrecta {

        @Test
        @DisplayName("marca el correo como verificado y guarda")
        void marca_el_correo_como_verificado() {
            when(repository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(sinVerificar()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(EMPLOYEE_ID);

            ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().isEmailVerified()).isTrue();
        }
    }

    @Nested
    class Rechazos {

        @Test
        @DisplayName("un empleado inexistente no escribe nada")
        void un_empleado_inexistente_no_escribe_nada() {
            when(repository.findById(EMPLOYEE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EMPLOYEE_ID))
                    .isInstanceOf(EmployeeNotFoundException.class);

            verify(repository, never()).save(any());
        }
    }
}
