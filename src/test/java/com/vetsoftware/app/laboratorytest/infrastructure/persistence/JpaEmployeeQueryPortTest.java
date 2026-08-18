package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.laboratorytest.domain.EmployeeRef;
import com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEmployeeQueryPort — adaptador sobre EmployeeJpaRepository")
class JpaEmployeeQueryPortTest {

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;
    @Mock
    private EmployeeJpaEntity employeeEntity;
    @InjectMocks
    private JpaEmployeeQueryPort port;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea el empleado encontrado a su companion VO")
        void mapea_el_empleado_encontrado_a_su_companion_vo() {
            EmployeeRef bacterioLoga = LaboratoryTestMother.BACTERIOLOGA;
            when(employeeEntity.getId()).thenReturn(bacterioLoga.id());
            when(employeeEntity.getEmployeeCode()).thenReturn(bacterioLoga.employeeCode());
            when(employeeEntity.getName()).thenReturn(bacterioLoga.name());
            when(employeeJpaRepository.findById(bacterioLoga.id()))
                    .thenReturn(Optional.of(employeeEntity));

            Optional<EmployeeRef> resultado = port.findById(bacterioLoga.id());

            assertThat(resultado).contains(bacterioLoga);
        }

        @Test
        @DisplayName("un empleado inexistente devuelve vacio")
        void un_empleado_inexistente_devuelve_vacio() {
            when(employeeJpaRepository.findById(999L)).thenReturn(Optional.empty());

            assertThat(port.findById(999L)).isEmpty();
        }
    }
}
