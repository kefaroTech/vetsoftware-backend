package com.vetsoftware.app.procedureschedule.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.procedureschedule.domain.EmployeeRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Adaptador de resolucion de empleado para el generador de tomas. A diferencia
 * de otras features, no filtra por empresa: el generador ya recibe el id del
 * empleado autenticado ({@code Authz.currentEmployeeId()}).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEmployeeQueryPort (procedureschedule)")
class JpaEmployeeQueryPortTest {

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;
    @InjectMocks
    private JpaEmployeeQueryPort port;

    private static EmployeeJpaEntity empleadoEncontrado(long id, String code, String name) {
        EmployeeJpaEntity entity = mock(EmployeeJpaEntity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getEmployeeCode()).thenReturn(code);
        when(entity.getName()).thenReturn(name);
        return entity;
    }

    @Test
    @DisplayName("mapea el empleado encontrado a su companion VO")
    void mapea_el_empleado_encontrado_a_su_companion_vo() {
        EmployeeJpaEntity empleado = empleadoEncontrado(7L, "EMP-001", "Ana Ruiz");
        when(employeeJpaRepository.findById(7L)).thenReturn(Optional.of(empleado));

        Optional<EmployeeRef> ref = port.findById(7L);

        assertThat(ref).contains(new EmployeeRef(7L, "EMP-001", "Ana Ruiz"));
    }

    @Test
    @DisplayName("devuelve vacio si el empleado no existe")
    void devuelve_vacio_si_el_empleado_no_existe() {
        when(employeeJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(port.findById(99L)).isEmpty();
    }
}
