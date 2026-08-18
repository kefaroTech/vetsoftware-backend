package com.vetsoftware.app.prescription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaPrescriberQueryPort")
class JpaPrescriberQueryPortTest {

    private static final Long EMPLOYEE_ID = 700L;

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;
    @Mock
    private EmployeeJpaEntity employeeEntity;

    @InjectMocks
    private JpaPrescriberQueryPort port;

    @Test
    @DisplayName("findName devuelve el nombre del empleado encontrado")
    void find_name_devuelve_el_nombre() {
        when(employeeJpaRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employeeEntity));
        when(employeeEntity.getName()).thenReturn("Dra. Ana Ruiz");

        Optional<String> result = port.findName(EMPLOYEE_ID);

        assertThat(result).contains("Dra. Ana Ruiz");
    }

    @Test
    @DisplayName("empleado inexistente devuelve vacio")
    void empleado_inexistente_devuelve_vacio() {
        when(employeeJpaRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.empty());

        assertThat(port.findName(EMPLOYEE_ID)).isEmpty();
    }
}
