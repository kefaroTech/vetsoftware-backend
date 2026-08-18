package com.vetsoftware.app.employeerole.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeRoleRepository;
import com.vetsoftware.app.employeerole.domain.EmployeeRoleNotFoundException;
import com.vetsoftware.app.employeerole.testsupport.EmployeeRoleMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindEmployeeRoleService")
class FindEmployeeRoleServiceTest {

    @Mock
    private EmployeeRoleRepository repository;

    @InjectMocks
    private FindEmployeeRoleService service;

    @Test
    @DisplayName("devuelve el dto de la asignacion encontrada")
    void devuelve_el_dto_de_la_asignacion_encontrada() {
        when(repository.findById(EmployeeRoleMother.EMPLOYEE_ROLE_ID))
                .thenReturn(Optional.of(EmployeeRoleMother.habilitado()));

        EmployeeRoleDto dto = service.findById(EmployeeRoleMother.EMPLOYEE_ROLE_ID);

        assertThat(dto.id()).isEqualTo(EmployeeRoleMother.EMPLOYEE_ROLE_ID);
        assertThat(dto.employee().id()).isEqualTo(EmployeeRoleMother.EMPLEADO.id());
    }

    @Test
    @DisplayName("una asignacion inexistente lanza EmployeeRoleNotFoundException")
    void asignacion_inexistente_lanza_not_found() {
        when(repository.findById(EmployeeRoleMother.EMPLOYEE_ROLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(EmployeeRoleMother.EMPLOYEE_ROLE_ID))
                .isInstanceOf(EmployeeRoleNotFoundException.class)
                .hasMessageContaining(String.valueOf(EmployeeRoleMother.EMPLOYEE_ROLE_ID));
    }
}
