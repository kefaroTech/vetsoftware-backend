package com.vetsoftware.app.rolepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import com.vetsoftware.app.rolepermission.domain.RolePermissionNotFoundException;
import com.vetsoftware.app.rolepermission.testsupport.RolePermissionMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Consultas de rolepermission")
class FindAndListRolePermissionsServiceTest {

    @Mock
    private RolePermissionRepository repository;

    @InjectMocks
    private FindRolePermissionService findService;
    @InjectMocks
    private ListRolePermissionsService listService;

    @Nested
    @DisplayName("FindRolePermissionService")
    class Buscar {

        @Test
        @DisplayName("devuelve el DTO de la asignacion encontrada")
        void devuelve_el_dto_de_la_asignacion() {
            when(repository.findById(1L)).thenReturn(Optional.of(RolePermissionMother.activa()));

            RolePermissionDto dto = findService.findById(1L);

            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.role().name()).isEqualTo("Veterinario");
            assertThat(dto.permission().name()).isEqualTo("Ver animales");
        }

        @Test
        @DisplayName("id inexistente levanta la excepcion de dominio con el id buscado")
        void id_inexistente_levanta_excepcion() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> findService.findById(99L))
                    .isInstanceOf(RolePermissionNotFoundException.class)
                    .hasMessageContaining("RolePermission not found: 99");
        }
    }

    @Nested
    @DisplayName("ListRolePermissionsService")
    class ListarTodo {

        @Test
        @DisplayName("mapea cada asignacion a su DTO conservando el orden")
        void mapea_cada_asignacion_conservando_el_orden() {
            when(repository.findAll()).thenReturn(
                    List.of(RolePermissionMother.conId(1L, RolePermissionMother.VER_ANIMALES),
                            RolePermissionMother.conId(2L, RolePermissionMother.CREAR_ANIMALES)));

            List<RolePermissionDto> dtos = listService.listAll();

            assertThat(dtos).extracting(RolePermissionDto::id).containsExactly(1L, 2L);
            assertThat(dtos).extracting(d -> d.permission().code()).containsExactly("ANIMAL_READ",
                    "ANIMAL_CREATE");
        }

        @Test
        @DisplayName("sin asignaciones devuelve lista vacia, no null")
        void sin_asignaciones_devuelve_lista_vacia() {
            when(repository.findAll()).thenReturn(List.of());

            assertThat(listService.listAll()).isEmpty();
        }
    }
}
