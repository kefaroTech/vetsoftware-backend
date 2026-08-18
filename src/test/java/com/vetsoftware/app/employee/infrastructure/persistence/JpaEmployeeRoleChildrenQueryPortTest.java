package com.vetsoftware.app.employee.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employeerole.infrastructure.persistence.EmployeeRoleJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEmployeeRoleChildrenQueryPort — adaptador sobre EmployeeRoleJpaRepository")
class JpaEmployeeRoleChildrenQueryPortTest {

    @Mock
    private EmployeeRoleJpaRepository jpaRepository;

    private JpaEmployeeRoleChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaEmployeeRoleChildrenQueryPort(jpaRepository);
    }

    @Test
    @DisplayName("un empleado con asignaciones de rol activas tiene hijos")
    void un_empleado_con_asignaciones_activas_tiene_hijos() {
        when(jpaRepository.existsByEmployee_Id(55L)).thenReturn(true);

        assertThat(port.existsActiveByEmployeeId(55L)).isTrue();
    }

    @Test
    @DisplayName("un empleado sin asignaciones de rol no tiene hijos")
    void un_empleado_sin_asignaciones_no_tiene_hijos() {
        when(jpaRepository.existsByEmployee_Id(55L)).thenReturn(false);

        assertThat(port.existsActiveByEmployeeId(55L)).isFalse();
    }
}
