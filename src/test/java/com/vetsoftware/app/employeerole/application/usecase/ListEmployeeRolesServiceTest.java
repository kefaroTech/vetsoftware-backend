package com.vetsoftware.app.employeerole.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeRoleRepository;
import com.vetsoftware.app.employeerole.testsupport.EmployeeRoleMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListEmployeeRolesService")
class ListEmployeeRolesServiceTest {

    @Mock
    private EmployeeRoleRepository repository;

    @InjectMocks
    private ListEmployeeRolesService service;

    @Test
    @DisplayName("devuelve la lista de asignaciones mapeadas a dto")
    void devuelve_la_lista_mapeada() {
        when(repository.findAll()).thenReturn(
                List.of(EmployeeRoleMother.habilitado(), EmployeeRoleMother.deOtroEmpleado()));

        List<EmployeeRoleDto> dtos = service.listAll();

        assertThat(dtos).hasSize(2);
        assertThat(dtos).extracting(dto -> dto.employee().name()).containsExactly("Ana Ruiz",
                "Luis Paz");
    }

    @Test
    @DisplayName("sin asignaciones devuelve una lista vacia")
    void sin_asignaciones_devuelve_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
