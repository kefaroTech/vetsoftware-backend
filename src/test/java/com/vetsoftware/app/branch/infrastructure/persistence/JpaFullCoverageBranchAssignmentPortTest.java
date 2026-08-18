package com.vetsoftware.app.branch.infrastructure.persistence;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employeebranch.infrastructure.persistence.EmployeeBranchJpaRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Adapter del auto-registro por sede: al crear una sucursal, los empleados "con
 * todas las sedes" heredan la nueva. Las dos ramas del {@code for} son la lista
 * vacía (nadie tiene cobertura total) y la lista con elementos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaFullCoverageBranchAssignmentPort")
class JpaFullCoverageBranchAssignmentPortTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long NEW_BRANCH_ID = 12L;

    @Mock
    private EmployeeBranchJpaRepository employeeBranchJpaRepository;

    @InjectMocks
    private JpaFullCoverageBranchAssignmentPort port;

    @Nested
    @DisplayName("assignNewBranchToFullCoverageEmployees")
    class Asignacion {

        @Test
        @DisplayName("inserta una fila por cada empleado con cobertura total")
        void inserta_una_fila_por_cada_empleado_con_cobertura_total() {
            when(employeeBranchJpaRepository.findFullCoverageEmployeeIds(COMPANY_ID, NEW_BRANCH_ID))
                    .thenReturn(List.of(101L, 102L));

            port.assignNewBranchToFullCoverageEmployees(COMPANY_ID, NEW_BRANCH_ID);

            verify(employeeBranchJpaRepository).insert(101L, NEW_BRANCH_ID, COMPANY_ID);
            verify(employeeBranchJpaRepository).insert(102L, NEW_BRANCH_ID, COMPANY_ID);
            verify(employeeBranchJpaRepository, times(2)).insert(anyLong(), eq(NEW_BRANCH_ID),
                    eq(COMPANY_ID));
        }

        @Test
        @DisplayName("sin empleados con cobertura total no inserta nada")
        void sin_empleados_con_cobertura_total_no_inserta_nada() {
            when(employeeBranchJpaRepository.findFullCoverageEmployeeIds(COMPANY_ID, NEW_BRANCH_ID))
                    .thenReturn(List.of());

            port.assignNewBranchToFullCoverageEmployees(COMPANY_ID, NEW_BRANCH_ID);

            verify(employeeBranchJpaRepository, never()).insert(anyLong(), anyLong(), anyLong());
        }
    }
}
