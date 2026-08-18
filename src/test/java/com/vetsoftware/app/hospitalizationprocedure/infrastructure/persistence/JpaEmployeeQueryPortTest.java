package com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.hospitalizationprocedure.domain.EmployeeRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEmployeeQueryPort")
class JpaEmployeeQueryPortTest {

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;
    @Mock
    private EmployeeJpaEntity entity;

    @InjectMocks
    private JpaEmployeeQueryPort port;

    @Test
    @DisplayName("empleado encontrado se mapea a EmployeeRef")
    void empleado_encontrado_se_mapea_a_employee_ref() {
        when(employeeJpaRepository.findById(4L)).thenReturn(Optional.of(entity));
        when(entity.getId()).thenReturn(4L);
        when(entity.getEmployeeCode()).thenReturn("EMP-001");
        when(entity.getName()).thenReturn("Ana Ruiz");

        Optional<EmployeeRef> resultado = port.findById(4L);

        assertThat(resultado).contains(new EmployeeRef(4L, "EMP-001", "Ana Ruiz"));
    }

    @Test
    @DisplayName("empleado inexistente devuelve vacio")
    void empleado_inexistente_devuelve_vacio() {
        when(employeeJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(port.findById(99L)).isEmpty();
    }
}
