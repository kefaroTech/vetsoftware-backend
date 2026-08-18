package com.vetsoftware.app.employee.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.domain.RoleSnapshot;
import com.vetsoftware.app.employeerole.infrastructure.persistence.EmployeeRoleJpaEntity;
import com.vetsoftware.app.employeerole.infrastructure.persistence.EmployeeRoleJpaRepository;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code EmployeeJpaEntity} y {@code RoleJpaEntity} se mockean: sus
 * constructores sin argumentos son {@code protected}, no son instanciables
 * desde este paquete y no tienen logica propia.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEmployeeRolesQueryPort — adaptador sobre EmployeeRoleJpaRepository")
class JpaEmployeeRolesQueryPortTest {

    @Mock
    private EmployeeRoleJpaRepository employeeRoleJpaRepository;
    @Mock
    private EmployeeRoleJpaEntity assignmentEntity;
    @Mock
    private EmployeeJpaEntity employeeEntity;
    @Mock
    private RoleJpaEntity roleEntity;

    private JpaEmployeeRolesQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaEmployeeRolesQueryPort(employeeRoleJpaRepository);
    }

    private void stubAsignacion() {
        when(assignmentEntity.getId()).thenReturn(500L);
        when(assignmentEntity.getEmployee()).thenReturn(employeeEntity);
        when(assignmentEntity.getRole()).thenReturn(roleEntity);
        when(employeeEntity.getId()).thenReturn(55L);
        when(roleEntity.getId()).thenReturn(3L);
        when(roleEntity.getName()).thenReturn("Veterinario");
        when(roleEntity.getCode()).thenReturn("VET");
    }

    @Nested
    @DisplayName("findRolesByEmployeeIds — asignaciones vigentes")
    class RolesVigentes {

        @Test
        @DisplayName("agrupa los snapshots de rol por id de empleado")
        void agrupa_los_snapshots_por_id_de_empleado() {
            stubAsignacion();
            when(employeeRoleJpaRepository.findByEmployeeIdIn(List.of(55L)))
                    .thenReturn(List.of(assignmentEntity));

            Map<Long, List<RoleSnapshot>> resultado = port.findRolesByEmployeeIds(List.of(55L));

            assertThat(resultado).containsOnlyKeys(55L);
            assertThat(resultado.get(55L))
                    .containsExactly(new RoleSnapshot(500L, 3L, "Veterinario", "VET"));
        }

        @Test
        @DisplayName("una lista de ids vacia no toca el repositorio")
        void una_lista_de_ids_vacia_no_toca_el_repositorio() {
            Map<Long, List<RoleSnapshot>> resultado = port.findRolesByEmployeeIds(List.of());

            assertThat(resultado).isEmpty();
            verifyNoInteractions(employeeRoleJpaRepository);
        }
    }

    @Nested
    @DisplayName("findRolesForListing — incluye desactivados")
    class RolesParaListado {

        @Test
        @DisplayName("agrupa tambien los snapshots de empleados desactivados")
        void agrupa_tambien_los_snapshots_de_desactivados() {
            stubAsignacion();
            when(employeeRoleJpaRepository.findForEmployeeListing(List.of(55L)))
                    .thenReturn(List.of(assignmentEntity));

            Map<Long, List<RoleSnapshot>> resultado = port.findRolesForListing(List.of(55L));

            assertThat(resultado.get(55L))
                    .containsExactly(new RoleSnapshot(500L, 3L, "Veterinario", "VET"));
        }

        @Test
        @DisplayName("una lista de ids vacia no toca el repositorio")
        void una_lista_de_ids_vacia_no_toca_el_repositorio() {
            Map<Long, List<RoleSnapshot>> resultado = port.findRolesForListing(List.of());

            assertThat(resultado).isEmpty();
            verifyNoInteractions(employeeRoleJpaRepository);
        }
    }
}
