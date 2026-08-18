package com.vetsoftware.app.employee.application.usecase;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.application.port.out.EmployeeRolesQueryPort;
import com.vetsoftware.app.employee.domain.AdminEmployeeCannotBeDisabledException;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import com.vetsoftware.app.employee.testsupport.EmployeeMother;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Baja (soft-delete) de un empleado: idempotente si ya estaba desactivado, y
 * bloqueada si el empleado tiene el rol ADMIN — perder al único administrador
 * dejaría la empresa sin nadie que gestione el resto.
 */
@ExtendWith(MockitoExtension.class)
class DeleteEmployeeServiceTest {

    private static final Long COMPANY_ID = 9L;

    @Mock
    private EmployeeRepository repository;
    @Mock
    private EmployeeRolesQueryPort employeeRolesQueryPort;
    @InjectMocks
    private DeleteEmployeeService service;

    @Nested
    class BajaCorrecta {

        @Test
        @DisplayName("desactiva un empleado habilitado sin rol admin")
        void desactiva_un_empleado_sin_rol_admin() {
            when(repository.findByIdIncludingDisabledAndCompanyId(EmployeeMother.EMPLOYEE_ID,
                    COMPANY_ID)).thenReturn(Optional.of(EmployeeMother.activo()));
            when(employeeRolesQueryPort.findRolesByEmployeeIds(List.of(EmployeeMother.EMPLOYEE_ID)))
                    .thenReturn(Map.of(EmployeeMother.EMPLOYEE_ID,
                            List.of(EmployeeMother.VETERINARIO)));

            service.execute(EmployeeMother.EMPLOYEE_ID, COMPANY_ID);

            verify(repository).delete(EmployeeMother.EMPLOYEE_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("companyId null es el camino SYSTEM: carga sin acotar")
        void companyId_null_usa_el_finder_ancho() {
            when(repository.findByIdIncludingDisabled(EmployeeMother.EMPLOYEE_ID))
                    .thenReturn(Optional.of(EmployeeMother.activo()));
            when(employeeRolesQueryPort.findRolesByEmployeeIds(List.of(EmployeeMother.EMPLOYEE_ID)))
                    .thenReturn(Map.of(EmployeeMother.EMPLOYEE_ID,
                            List.of(EmployeeMother.VETERINARIO)));

            service.execute(EmployeeMother.EMPLOYEE_ID, null);

            verify(repository).delete(EmployeeMother.EMPLOYEE_ID, null);
        }
    }

    @Nested
    class Rechazos {

        @Test
        @DisplayName("un empleado inexistente lanza EmployeeNotFoundException")
        void un_empleado_inexistente_lanza_not_found() {
            when(repository.findByIdIncludingDisabledAndCompanyId(EmployeeMother.EMPLOYEE_ID,
                    COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EmployeeMother.EMPLOYEE_ID, COMPANY_ID))
                    .isInstanceOf(EmployeeNotFoundException.class);

            verify(repository, never()).delete(anyLong(), anyLong());
            verifyNoInteractions(employeeRolesQueryPort);
        }

        @Test
        @DisplayName("un empleado ya desactivado no hace nada — idempotente")
        void un_empleado_ya_desactivado_es_idempotente() {
            when(repository.findByIdIncludingDisabledAndCompanyId(EmployeeMother.EMPLOYEE_ID,
                    COMPANY_ID)).thenReturn(Optional.of(EmployeeMother.deshabilitado()));

            assertThatCode(() -> service.execute(EmployeeMother.EMPLOYEE_ID, COMPANY_ID))
                    .doesNotThrowAnyException();

            verify(repository, never()).delete(anyLong(), anyLong());
            verifyNoInteractions(employeeRolesQueryPort);
        }

        @Test
        @DisplayName("el empleado de otra empresa es un 404 y no se desactiva")
        void un_empleado_de_otra_empresa_no_se_desactiva() {
            // La lectura previa va al finder acotado: la fila del otro tenant no se
            // devuelve, asi que el servicio ni llega al UPDATE. Es la fuga que la regla
            // OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM marcaba en DeleteEmployeeUseCase.
            when(repository.findByIdIncludingDisabledAndCompanyId(EmployeeMother.EMPLOYEE_ID,
                    COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EmployeeMother.EMPLOYEE_ID, COMPANY_ID))
                    .isInstanceOf(EmployeeNotFoundException.class);

            verify(repository, never()).findByIdIncludingDisabled(anyLong());
            verify(repository, never()).delete(anyLong(), anyLong());
            verifyNoInteractions(employeeRolesQueryPort);
        }

        @Test
        @DisplayName("un empleado con rol ADMIN no se puede desactivar")
        void un_empleado_con_rol_admin_no_se_puede_desactivar() {
            when(repository.findByIdIncludingDisabledAndCompanyId(EmployeeMother.EMPLOYEE_ID,
                    COMPANY_ID)).thenReturn(Optional.of(EmployeeMother.activo()));
            when(employeeRolesQueryPort.findRolesByEmployeeIds(List.of(EmployeeMother.EMPLOYEE_ID)))
                    .thenReturn(Map.of(EmployeeMother.EMPLOYEE_ID,
                            List.of(EmployeeMother.ADMINISTRADOR)));

            assertThatThrownBy(() -> service.execute(EmployeeMother.EMPLOYEE_ID, COMPANY_ID))
                    .isInstanceOf(AdminEmployeeCannotBeDisabledException.class)
                    .hasMessageContaining("ADMIN");

            verify(repository, never()).delete(anyLong(), anyLong());
        }
    }
}
