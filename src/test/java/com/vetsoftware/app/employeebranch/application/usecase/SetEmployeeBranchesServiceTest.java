package com.vetsoftware.app.employeebranch.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employeebranch.application.command.SetEmployeeBranchesCommand;
import com.vetsoftware.app.employeebranch.application.dto.EmployeeBranchesDto;
import com.vetsoftware.app.employeebranch.application.port.out.BranchAccessCachePort;
import com.vetsoftware.app.employeebranch.application.port.out.BranchQueryPort;
import com.vetsoftware.app.employeebranch.application.port.out.EmployeeBranchRepository;
import com.vetsoftware.app.employeebranch.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.employeebranch.testsupport.EmployeeBranchMother;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SetEmployeeBranchesService")
class SetEmployeeBranchesServiceTest {

    private static final Long EMPLOYEE_ID = EmployeeBranchMother.EMPLOYEE_ID;
    private static final Long COMPANY_ID = EmployeeBranchMother.COMPANY_ID;
    private static final Long BRANCH_1 = EmployeeBranchMother.BRANCH_1;
    private static final Long BRANCH_2 = EmployeeBranchMother.BRANCH_2;
    private static final Long BRANCH_3 = EmployeeBranchMother.BRANCH_3;

    @Mock
    private EmployeeBranchRepository repository;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private BranchQueryPort branchQueryPort;
    @Mock
    private BranchAccessCachePort cachePort;

    @InjectMocks
    private SetEmployeeBranchesService service;

    @Captor
    private ArgumentCaptor<Collection<Long>> branchesCaptor;

    private void elEmpleadoExiste() {
        when(employeeQueryPort.existsByIdAndCompanyId(EMPLOYEE_ID, COMPANY_ID)).thenReturn(true);
    }

    @Nested
    @DisplayName("reemplazo del set de sedes")
    class Reemplazo {

        @Test
        @DisplayName("persiste exactamente las sedes pedidas y evicta el cache")
        void persiste_exactamente_las_sedes_pedidas() {
            elEmpleadoExiste();
            when(branchQueryPort.findBranchIdsByCompanyId(COMPANY_ID))
                    .thenReturn(List.of(BRANCH_1, BRANCH_2, BRANCH_3));

            EmployeeBranchesDto dto = service
                    .execute(EmployeeBranchMother.comandoConSedes(BRANCH_1, BRANCH_2));

            verify(repository).replaceBranches(eq(EMPLOYEE_ID), eq(COMPANY_ID),

                    branchesCaptor.capture());
            assertThat(branchesCaptor.getValue()).containsExactly(BRANCH_1, BRANCH_2);
            assertThat(dto.employeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(dto.branchIds()).containsExactly(BRANCH_1, BRANCH_2);
            verify(cachePort).evictByEmployeeId(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("allBranches=true expande a todas las sedes de la empresa, no a un alcance parcial")
        void all_branches_expande_a_todas_las_sedes_de_la_empresa() {
            elEmpleadoExiste();
            when(branchQueryPort.findBranchIdsByCompanyId(COMPANY_ID))
                    .thenReturn(List.of(BRANCH_1, BRANCH_2, BRANCH_3));

            EmployeeBranchesDto dto = service.execute(EmployeeBranchMother.comandoTodasLasSedes());

            verify(repository).replaceBranches(eq(EMPLOYEE_ID), eq(COMPANY_ID),

                    branchesCaptor.capture());
            assertThat(branchesCaptor.getValue()).containsExactly(BRANCH_1, BRANCH_2, BRANCH_3);
            assertThat(dto.branchIds()).containsExactly(BRANCH_1, BRANCH_2, BRANCH_3);
        }

        @Test
        @DisplayName("una sede repetida en el pedido no se duplica en lo persistido")
        void una_sede_repetida_no_se_duplica() {
            elEmpleadoExiste();
            when(branchQueryPort.findBranchIdsByCompanyId(COMPANY_ID))
                    .thenReturn(List.of(BRANCH_1, BRANCH_2, BRANCH_3));

            EmployeeBranchesDto dto = service
                    .execute(EmployeeBranchMother.comandoConSedes(BRANCH_2, BRANCH_1, BRANCH_2));

            // LinkedHashSet: dedup manteniendo el orden de la primera aparicion.
            assertThat(dto.branchIds()).containsExactly(BRANCH_2, BRANCH_1);
        }
    }

    @Nested
    @DisplayName("validaciones que impiden escribir")
    class Validaciones {

        @Test
        @DisplayName("empleado inexistente: no consulta sedes ni escribe nada")
        void empleado_inexistente_no_consulta_nada_mas() {
            when(employeeQueryPort.existsByIdAndCompanyId(EMPLOYEE_ID, COMPANY_ID))
                    .thenReturn(false);

            assertThatThrownBy(
                    () -> service.execute(EmployeeBranchMother.comandoConSedes(BRANCH_1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: " + EMPLOYEE_ID);

            verifyNoInteractions(branchQueryPort, repository, cachePort);
        }

        @Test
        @DisplayName("una sede que no pertenece a la empresa aborta antes de escribir")
        void sede_de_otra_empresa_no_persiste() {
            elEmpleadoExiste();
            when(branchQueryPort.findBranchIdsByCompanyId(COMPANY_ID))
                    .thenReturn(List.of(BRANCH_1));

            assertThatThrownBy(
                    () -> service.execute(EmployeeBranchMother.comandoConSedes(BRANCH_1, 999L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Branch not found: 999");

            verifyNoInteractions(repository, cachePort);
        }

        @Test
        @DisplayName("una lista vacia de sedes no persiste: hay que dejar al menos una")
        void lista_vacia_de_sedes_no_persiste() {
            elEmpleadoExiste();
            when(branchQueryPort.findBranchIdsByCompanyId(COMPANY_ID))
                    .thenReturn(List.of(BRANCH_1));

            assertThatThrownBy(() -> service.execute(EmployeeBranchMother.comandoConSedes()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one branch is required");

            verifyNoInteractions(repository, cachePort);
        }

        @Test
        @DisplayName("branchIds null sin allBranches tambien deja el set vacio y aborta")
        void branch_ids_null_sin_all_branches_no_persiste() {
            elEmpleadoExiste();
            when(branchQueryPort.findBranchIdsByCompanyId(COMPANY_ID))
                    .thenReturn(List.of(BRANCH_1));
            SetEmployeeBranchesCommand comando = new SetEmployeeBranchesCommand(EMPLOYEE_ID,
                    COMPANY_ID, false, null);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one branch is required");

            verifyNoInteractions(repository, cachePort);
        }
    }
}
