package com.vetsoftware.app.employeerole.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.employeerole.domain.EmployeeRef;
import com.vetsoftware.app.employeerole.testsupport.EmployeeRoleMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEmployeeQueryPort (employeerole) — resuelve el EmployeeRef desde el repositorio Spring Data")
class JpaEmployeeQueryPortTest {

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;

    @InjectMocks
    private JpaEmployeeQueryPort port;

    private static EmployeeJpaEntity entidad(Long id, String code, String name) {
        EmployeeJpaEntity e = mock(EmployeeJpaEntity.class);
        when(e.getId()).thenReturn(id);
        when(e.getEmployeeCode()).thenReturn(code);
        when(e.getName()).thenReturn(name);
        return e;
    }

    @Test
    @DisplayName("mapea el empleado encontrado al EmployeeRef")
    void mapea_el_empleado_encontrado() {
        EmployeeJpaEntity entity = entidad(EmployeeRoleMother.EMPLEADO.id(),
                EmployeeRoleMother.EMPLEADO.employeeCode(), EmployeeRoleMother.EMPLEADO.name());
        when(employeeJpaRepository.findById(EmployeeRoleMother.EMPLEADO.id()))
                .thenReturn(Optional.of(entity));

        Optional<EmployeeRef> result = port.findById(EmployeeRoleMother.EMPLEADO.id());

        assertThat(result).contains(EmployeeRoleMother.EMPLEADO);
    }

    @Test
    @DisplayName("vacio si el empleado no existe")
    void vacio_si_el_empleado_no_existe() {
        when(employeeJpaRepository.findById(EmployeeRoleMother.EMPLEADO.id()))
                .thenReturn(Optional.empty());

        assertThat(port.findById(EmployeeRoleMother.EMPLEADO.id())).isEmpty();
    }

    @Test
    @DisplayName("la variante acotada consulta por id Y empresa, no solo por id")
    void la_variante_acotada_consulta_por_id_y_empresa() {
        EmployeeJpaEntity entity = entidad(EmployeeRoleMother.EMPLEADO.id(),
                EmployeeRoleMother.EMPLEADO.employeeCode(), EmployeeRoleMother.EMPLEADO.name());
        when(employeeJpaRepository.findByIdAndCompany_Id(EmployeeRoleMother.EMPLEADO.id(),
                EmployeeRoleMother.EMPRESA)).thenReturn(Optional.of(entity));

        Optional<EmployeeRef> result = port.findByIdAndCompanyId(EmployeeRoleMother.EMPLEADO.id(),
                EmployeeRoleMother.EMPRESA);

        assertThat(result).contains(EmployeeRoleMother.EMPLEADO);
    }

    /** El empleado de otro tenant sale vacio: es la barrera de W6. */
    @Test
    @DisplayName("vacio si el empleado es de otra empresa")
    void vacio_si_el_empleado_es_de_otra_empresa() {
        when(employeeJpaRepository.findByIdAndCompany_Id(EmployeeRoleMother.EMPLEADO.id(),
                EmployeeRoleMother.EMPRESA)).thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(EmployeeRoleMother.EMPLEADO.id(),
                EmployeeRoleMother.EMPRESA)).isEmpty();
    }
}
