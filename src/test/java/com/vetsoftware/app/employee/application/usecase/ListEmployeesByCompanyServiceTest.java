package com.vetsoftware.app.employee.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.out.EmployeeBranchesQueryPort;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.application.port.out.EmployeeRolesQueryPort;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.testsupport.EmployeeMother;
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
 * Listado de empleados de la empresa del contexto (incluye desactivados: la
 * pantalla muestra su estado). Es el listado que sirve al tenant — el
 * aislamiento aquí es que una empresa sin empleados propios ve una lista vacía
 * aunque existan empleados de otras empresas.
 */
@ExtendWith(MockitoExtension.class)
class ListEmployeesByCompanyServiceTest {

    @Mock
    private EmployeeRepository repository;
    @Mock
    private EmployeeRolesQueryPort rolesQueryPort;
    @Mock
    private EmployeeBranchesQueryPort branchesQueryPort;
    @InjectMocks
    private ListEmployeesByCompanyService service;

    @Nested
    class Listado {

        @Test
        @DisplayName("trae los roles y sedes de la pagina en una consulta batch por ids")
        void trae_roles_y_sedes_en_batch() {
            Employee empleado = EmployeeMother.activo();
            when(repository.findAllByCompanyIdIncludingDisabled(EmployeeMother.COMPANY_ID))
                    .thenReturn(List.of(empleado));
            when(rolesQueryPort.findRolesForListing(List.of(EmployeeMother.EMPLOYEE_ID)))
                    .thenReturn(Map.of(EmployeeMother.EMPLOYEE_ID,
                            List.of(EmployeeMother.VETERINARIO)));
            when(branchesQueryPort.findBranchesByEmployeeIds(List.of(EmployeeMother.EMPLOYEE_ID)))
                    .thenReturn(
                            Map.of(EmployeeMother.EMPLOYEE_ID, List.of(EmployeeMother.SEDE_NORTE)));

            List<EmployeeDto> resultado = service.listByCompany(EmployeeMother.COMPANY_ID);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).roles()).extracting("code").containsExactly("VET");
            assertThat(resultado.get(0).branches()).extracting("name")
                    .containsExactly("Sede Norte");
        }
    }

    @Nested
    class Tenancy {

        @Test
        @DisplayName("una empresa sin empleados propios ve una lista vacia")
        void una_empresa_sin_empleados_propios_ve_lista_vacia() {
            when(repository.findAllByCompanyIdIncludingDisabled(EmployeeMother.OTRA_COMPANY_ID))
                    .thenReturn(List.of());
            when(rolesQueryPort.findRolesForListing(List.of())).thenReturn(Map.of());
            when(branchesQueryPort.findBranchesByEmployeeIds(List.of())).thenReturn(Map.of());

            List<EmployeeDto> resultado = service.listByCompany(EmployeeMother.OTRA_COMPANY_ID);

            assertThat(resultado).isEmpty();
        }
    }
}
