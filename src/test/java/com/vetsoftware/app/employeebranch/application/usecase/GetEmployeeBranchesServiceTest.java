package com.vetsoftware.app.employeebranch.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employeebranch.application.dto.EmployeeBranchesDto;
import com.vetsoftware.app.employeebranch.application.port.out.EmployeeBranchRepository;
import com.vetsoftware.app.employeebranch.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.employeebranch.testsupport.EmployeeBranchMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetEmployeeBranchesService")
class GetEmployeeBranchesServiceTest {

    private static final Long EMPLOYEE_ID = EmployeeBranchMother.EMPLOYEE_ID;
    private static final Long COMPANY_ID = EmployeeBranchMother.COMPANY_ID;

    @Mock
    private EmployeeBranchRepository repository;
    @Mock
    private EmployeeQueryPort employeeQueryPort;

    @InjectMocks
    private GetEmployeeBranchesService service;

    @Test
    @DisplayName("devuelve las sedes vigentes de un empleado que existe en la empresa")
    void devuelve_las_sedes_vigentes_del_empleado() {
        when(employeeQueryPort.existsByIdAndCompanyId(EMPLOYEE_ID, COMPANY_ID)).thenReturn(true);
        when(repository.findBranchIdsByEmployeeId(EMPLOYEE_ID))
                .thenReturn(List.of(EmployeeBranchMother.BRANCH_1, EmployeeBranchMother.BRANCH_2));

        EmployeeBranchesDto dto = service.execute(EMPLOYEE_ID, COMPANY_ID);

        assertThat(dto.employeeId()).isEqualTo(EMPLOYEE_ID);
        assertThat(dto.branchIds()).containsExactly(EmployeeBranchMother.BRANCH_1,
                EmployeeBranchMother.BRANCH_2);
    }

    @Test
    @DisplayName("un empleado sin ninguna sede asignada devuelve una lista vacia, no null")
    void empleado_sin_sedes_devuelve_lista_vacia() {
        when(employeeQueryPort.existsByIdAndCompanyId(EMPLOYEE_ID, COMPANY_ID)).thenReturn(true);
        when(repository.findBranchIdsByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of());

        assertThat(service.execute(EMPLOYEE_ID, COMPANY_ID).branchIds()).isEmpty();
    }

    @Test
    @DisplayName("empleado inexistente en la empresa: no llega a consultar sus sedes")
    void empleado_inexistente_no_consulta_las_sedes() {
        when(employeeQueryPort.existsByIdAndCompanyId(EMPLOYEE_ID, COMPANY_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.execute(EMPLOYEE_ID, COMPANY_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Employee not found: " + EMPLOYEE_ID);

        verifyNoInteractions(repository);
    }
}
