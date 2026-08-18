package com.vetsoftware.app.openaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.openaccount.domain.EmployeeRef;
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
    @InjectMocks
    private JpaEmployeeQueryPort port;

    @Test
    @DisplayName("mapea el empleado encontrado a EmployeeRef")
    void mapea_el_empleado_encontrado() {
        EmployeeJpaEntity entity = mock(EmployeeJpaEntity.class);
        when(entity.getId()).thenReturn(4L);
        when(entity.getName()).thenReturn("Dra. Vet");
        when(employeeJpaRepository.findById(4L)).thenReturn(Optional.of(entity));

        assertThat(port.findById(4L)).contains(new EmployeeRef(4L, "Dra. Vet"));
    }

    @Test
    @DisplayName("devuelve vacio si el empleado no existe")
    void devuelve_vacio_si_no_existe() {
        when(employeeJpaRepository.findById(4L)).thenReturn(Optional.empty());

        assertThat(port.findById(4L)).isEmpty();
    }
}
