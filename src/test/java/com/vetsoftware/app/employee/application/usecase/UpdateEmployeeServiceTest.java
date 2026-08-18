package com.vetsoftware.app.employee.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.application.command.UpdateEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import com.vetsoftware.app.employee.testsupport.EmployeeMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateEmployeeServiceTest {

    private static final Long EMPRESA = EmployeeMother.COMPANY_ID;
    private static final Long ID = EmployeeMother.EMPLOYEE_ID;

    @Mock
    private EmployeeRepository repository;
    @InjectMocks
    private UpdateEmployeeService service;

    @Nested
    class ActualizacionCorrecta {

        @Test
        @DisplayName("actualiza codigo, nombre y correo y guarda")
        void actualiza_y_guarda() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeMother.activo()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EmployeeDto dto = service.execute(EmployeeMother.comandoActualizar());

            assertThat(dto.employeeCode()).isEqualTo("VV-NUEVO");
            assertThat(dto.name()).isEqualTo("Mariana Rojas Perez");
            assertThat(dto.email()).isEqualTo("mariana.rojas@vetrina.co");
        }

        @Test
        @DisplayName("sin empresa en el contexto (SYSTEM) carga sin acotar")
        void sin_empresa_carga_sin_acotar() {
            when(repository.findById(ID)).thenReturn(Optional.of(EmployeeMother.activo()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(new UpdateEmployeeCommand(ID, "VV-NUEVO", "Mariana Rojas Perez",
                    "mariana.rojas@vetrina.co", null));

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }
    }

    @Nested
    class Tenancy {

        /**
         * El defecto que cierra este test: el {@code @authz.isMyCompany} del puerto
         * solo prueba que el atacante declara SU empresa. Cargando por id a secas, la
         * escritura caía sobre el empleado de otra.
         */
        @Test
        @DisplayName("un empleado de otra empresa no se lee ni se guarda")
        void un_empleado_de_otra_empresa_no_se_guarda() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EmployeeMother.comandoActualizar()))
                    .isInstanceOf(EmployeeNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));

            verify(repository, never()).findById(any());
            verify(repository, never()).save(any());
        }
    }

    @Nested
    class Rechazos {

        @Test
        @DisplayName("un empleado inexistente no escribe nada")
        void un_empleado_inexistente_no_escribe_nada() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EmployeeMother.comandoActualizar()))
                    .isInstanceOf(EmployeeNotFoundException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un codigo en blanco no se guarda — lo valida el propio agregado")
        void un_codigo_en_blanco_no_se_guarda() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeMother.activo()));
            UpdateEmployeeCommand command = new UpdateEmployeeCommand(ID, "  ", "Mariana",
                    "mariana@vetrina.co", EMPRESA);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(repository, never()).save(any());
        }
    }
}
