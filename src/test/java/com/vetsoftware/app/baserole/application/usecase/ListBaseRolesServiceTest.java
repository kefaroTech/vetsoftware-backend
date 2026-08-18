package com.vetsoftware.app.baserole.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import com.vetsoftware.app.baserole.application.port.out.BaseRoleRepository;
import com.vetsoftware.app.baserole.testsupport.BaseRoleMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListBaseRolesService")
class ListBaseRolesServiceTest {

    @Mock
    private BaseRoleRepository repository;

    @InjectMocks
    private ListBaseRolesService service;

    @Test
    @DisplayName("mapea todos los roles base a dto")
    void mapea_todos_los_roles_base_a_dto() {
        when(repository.findAll())
                .thenReturn(List.of(BaseRoleMother.veterinario(), BaseRoleMother.administrador()));

        List<BaseRoleDto> result = service.listAll();

        assertThat(result).extracting(BaseRoleDto::name).containsExactly("Veterinario",
                "Administrador");
    }

    @Test
    @DisplayName("un repositorio vacio devuelve una lista vacia")
    void un_repositorio_vacio_devuelve_una_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
