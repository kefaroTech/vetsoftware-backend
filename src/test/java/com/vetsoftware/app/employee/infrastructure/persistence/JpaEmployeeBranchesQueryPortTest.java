package com.vetsoftware.app.employee.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.domain.BranchRef;
import com.vetsoftware.app.employeebranch.infrastructure.persistence.EmployeeBranchAssignmentView;
import com.vetsoftware.app.employeebranch.infrastructure.persistence.EmployeeBranchJpaRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEmployeeBranchesQueryPort — adaptador sobre EmployeeBranchJpaRepository")
class JpaEmployeeBranchesQueryPortTest {

    @Mock
    private EmployeeBranchJpaRepository employeeBranchJpaRepository;
    @Mock
    private EmployeeBranchAssignmentView sur;
    @Mock
    private EmployeeBranchAssignmentView norte;

    private JpaEmployeeBranchesQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaEmployeeBranchesQueryPort(employeeBranchJpaRepository);
    }

    @Nested
    @DisplayName("findBranchesByEmployeeIds")
    class Busqueda {

        @Test
        @DisplayName("agrupa por empleado y ordena las sedes por nombre sin distinguir mayusculas")
        void agrupa_por_empleado_y_ordena_por_nombre_case_insensitive() {
            when(sur.getEmployeeId()).thenReturn(55L);
            when(sur.getBranchId()).thenReturn(8L);
            when(sur.getBranchName()).thenReturn("sede sur");
            when(norte.getEmployeeId()).thenReturn(55L);
            when(norte.getBranchId()).thenReturn(7L);
            when(norte.getBranchName()).thenReturn("Sede Norte");
            when(employeeBranchJpaRepository.findAssignmentsByEmployeeIds(List.of(55L)))
                    .thenReturn(List.of(sur, norte));

            Map<Long, List<BranchRef>> resultado = port.findBranchesByEmployeeIds(List.of(55L));

            assertThat(resultado.get(55L)).extracting(BranchRef::name).containsExactly("Sede Norte",
                    "sede sur");
        }

        @Test
        @DisplayName("una lista de ids vacia no toca el repositorio")
        void una_lista_de_ids_vacia_no_toca_el_repositorio() {
            Map<Long, List<BranchRef>> resultado = port.findBranchesByEmployeeIds(List.of());

            assertThat(resultado).isEmpty();
            verifyNoInteractions(employeeBranchJpaRepository);
        }
    }
}
