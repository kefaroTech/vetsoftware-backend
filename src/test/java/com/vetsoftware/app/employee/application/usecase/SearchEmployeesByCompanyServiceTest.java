package com.vetsoftware.app.employee.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.application.command.SearchEmployeesCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.out.EmployeeBranchesQueryPort;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.application.port.out.EmployeeRolesQueryPort;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.testsupport.EmployeeMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Búsqueda paginada de empleados por empresa. El total y el número de página
 * son los de la consulta, no se recalculan sobre el contenido ya mapeado.
 */
@ExtendWith(MockitoExtension.class)
class SearchEmployeesByCompanyServiceTest {

    @Mock
    private EmployeeRepository repository;
    @Mock
    private EmployeeRolesQueryPort rolesQueryPort;
    @Mock
    private EmployeeBranchesQueryPort branchesQueryPort;
    @InjectMocks
    private SearchEmployeesByCompanyService service;

    @Nested
    class Busqueda {

        @Test
        @DisplayName("conserva los metadatos de paginacion de la consulta al mapear a dto")
        void conserva_los_metadatos_de_paginacion() {
            Employee empleado = EmployeeMother.activo();
            SearchEmployeesCommand command = new SearchEmployeesCommand(EmployeeMother.COMPANY_ID,
                    "mariana", 0, 15);
            PageResult<Employee> pagina = new PageResult<>(List.of(empleado), 0, 15, 1, 1);
            when(repository.search(command)).thenReturn(pagina);
            when(rolesQueryPort.findRolesForListing(List.of(EmployeeMother.EMPLOYEE_ID)))
                    .thenReturn(Map.of(EmployeeMother.EMPLOYEE_ID, List.of(EmployeeMother.CAJERO)));
            when(branchesQueryPort.findBranchesByEmployeeIds(List.of(EmployeeMother.EMPLOYEE_ID)))
                    .thenReturn(Map.of());

            PageResult<EmployeeDto> resultado = service.search(command);

            assertThat(resultado.totalElements()).isEqualTo(1);
            assertThat(resultado.totalPages()).isEqualTo(1);
            assertThat(resultado.content()).extracting(EmployeeDto::id)
                    .containsExactly(EmployeeMother.EMPLOYEE_ID);
            assertThat(resultado.content().get(0).roles()).extracting("code")
                    .containsExactly("CASHIER");
        }

        @Test
        @DisplayName("una pagina vacia no consulta roles ni sedes por ids")
        void una_pagina_vacia_no_arrastra_roles_ni_sedes() {
            SearchEmployeesCommand command = new SearchEmployeesCommand(EmployeeMother.COMPANY_ID,
                    "zzz", 0, 15);
            when(repository.search(command)).thenReturn(new PageResult<>(List.of(), 0, 15, 0, 0));
            when(rolesQueryPort.findRolesForListing(List.of())).thenReturn(Map.of());
            when(branchesQueryPort.findBranchesByEmployeeIds(List.of())).thenReturn(Map.of());

            PageResult<EmployeeDto> resultado = service.search(command);

            assertThat(resultado.content()).isEmpty();
            assertThat(resultado.totalElements()).isZero();
        }
    }
}
