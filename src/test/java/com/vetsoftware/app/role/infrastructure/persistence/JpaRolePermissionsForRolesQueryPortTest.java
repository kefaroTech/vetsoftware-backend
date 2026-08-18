package com.vetsoftware.app.role.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaEntity;
import com.vetsoftware.app.role.application.dto.PermissionSummaryDto;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaEntity;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaRolePermissionsForRolesQueryPort — adaptador sobre RolePermissionJpaRepository")
class JpaRolePermissionsForRolesQueryPortTest {

    @Mock
    private RolePermissionJpaRepository repository;

    @InjectMocks
    private JpaRolePermissionsForRolesQueryPort port;

    private static RoleJpaEntity roleEntity(Long id) {
        RoleJpaEntity entity = new RoleJpaEntity();
        entity.setId(id);
        return entity;
    }

    private static PermissionJpaEntity permissionEntity(Long id, String name, String code) {
        PermissionJpaEntity entity = new PermissionJpaEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setCode(code);
        return entity;
    }

    private static RolePermissionJpaEntity rolePermissionEntity(Long id, RoleJpaEntity role,
            PermissionJpaEntity permission) {
        RolePermissionJpaEntity entity = new RolePermissionJpaEntity();
        entity.setId(id);
        entity.setRole(role);
        entity.setPermission(permission);
        return entity;
    }

    @Nested
    @DisplayName("findByRoleIds")
    class FindByRoleIds {

        @Test
        @DisplayName("una lista vacia de roles no consulta el repositorio")
        void una_lista_vacia_no_consulta_el_repositorio() {
            Map<Long, List<PermissionSummaryDto>> resultado = port.findByRoleIds(List.of());

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("agrupa los permisos de varios roles por el id del rol")
        void agrupa_los_permisos_por_rol() {
            RoleJpaEntity veterinario = roleEntity(1L);
            RoleJpaEntity administrador = roleEntity(2L);
            when(repository.findByRoleIdIn(List.of(1L, 2L))).thenReturn(List.of(
                    rolePermissionEntity(10L, veterinario,
                            permissionEntity(100L, "Ver animales", "ANIMAL_READ")),
                    rolePermissionEntity(11L, administrador,
                            permissionEntity(101L, "Crear animales", "ANIMAL_CREATE"))));

            Map<Long, List<PermissionSummaryDto>> resultado = port.findByRoleIds(List.of(1L, 2L));

            assertThat(resultado.get(1L)).containsExactly(
                    new PermissionSummaryDto(10L, 100L, "Ver animales", "ANIMAL_READ"));
            assertThat(resultado.get(2L)).containsExactly(
                    new PermissionSummaryDto(11L, 101L, "Crear animales", "ANIMAL_CREATE"));
        }
    }
}
