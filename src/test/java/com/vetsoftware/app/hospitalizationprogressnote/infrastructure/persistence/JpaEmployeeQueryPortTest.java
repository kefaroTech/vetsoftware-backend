package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.hospitalizationprogressnote.domain.EmployeeRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Adaptador de resolucion de empleado para las notas de evolucion. Sin filtro
 * de empresa: el companion VO solo trae codigo y nombre para mostrar y quien
 * llama ya resuelve la hospitalizacion con su propio filtro.
 */
@ExtendWith(MockitoExtension.class)
class JpaEmployeeQueryPortTest {

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;
    @InjectMocks
    private JpaEmployeeQueryPort port;

    private static EmployeeJpaEntity empleadoEncontrado(long id, String codigo, String nombre) {
        EmployeeJpaEntity entity = mock(EmployeeJpaEntity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getEmployeeCode()).thenReturn(codigo);
        when(entity.getName()).thenReturn(nombre);
        return entity;
    }

    @Test
    @DisplayName("mapea el empleado encontrado a su companion VO")
    void mapea_el_empleado_encontrado_a_su_companion_vo() {
        EmployeeJpaEntity empleado = empleadoEncontrado(4L, "EMP-001", "Ana Ruiz");
        when(employeeJpaRepository.findById(4L)).thenReturn(Optional.of(empleado));

        Optional<EmployeeRef> ref = port.findById(4L);

        assertThat(ref).contains(new EmployeeRef(4L, "EMP-001", "Ana Ruiz"));
    }

    @Test
    @DisplayName("devuelve vacio si el empleado no existe")
    void devuelve_vacio_si_el_empleado_no_existe() {
        when(employeeJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(port.findById(99L)).isEmpty();
    }
}
