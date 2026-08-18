package com.vetsoftware.app.role.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.testsupport.RoleMother;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListRolesService")
class ListRolesServiceTest {

    @Mock
    private RoleRepository repository;

    private ListRolesService service;

    @BeforeEach
    void crearServicio() {
        service = new ListRolesService(repository);
    }

    @Nested
    @DisplayName("listado global")
    class ListadoGlobal {

        @Test
        @DisplayName("mapea todos los roles del repositorio a dto")
        void mapea_todos_los_roles_a_dto() {
            when(repository.findAll())
                    .thenReturn(List.of(RoleMother.veterinario(), RoleMother.administrador()));

            List<RoleDto> resultado = service.listAll();

            assertThat(resultado).extracting(RoleDto::name).containsExactly("Veterinario",
                    "Administrador");
        }

        @Test
        @DisplayName("sin roles, devuelve una lista vacia")
        void sin_roles_devuelve_lista_vacia() {
            when(repository.findAll()).thenReturn(List.of());

            assertThat(service.listAll()).isEmpty();
        }
    }
}
