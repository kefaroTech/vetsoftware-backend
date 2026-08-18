package com.vetsoftware.app.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.testsupport.ReflectionEntities;
import com.vetsoftware.app.employeerole.infrastructure.persistence.EmployeeRoleJpaEntity;
import com.vetsoftware.app.employeerole.infrastructure.persistence.EmployeeRoleJpaRepository;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaEntity;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code EmployeeRoleJpaEntity} y {@code RolePermissionJpaEntity} tienen
 * constructor público y se construyen reales; sus {@code RoleJpaEntity} y
 * {@code PermissionJpaEntity} asociadas tienen constructor protegido de otra
 * feature y se construyen por reflexión (ver {@code ReflectionEntities}).
 */
@ExtendWith(MockitoExtension.class)
class JpaPermissionResolverTest {

    @Mock
    private EmployeeRoleJpaRepository employeeRoleJpaRepository;
    @Mock
    private RolePermissionJpaRepository rolePermissionJpaRepository;
    @InjectMocks
    private JpaPermissionResolver resolver;

    private static EmployeeRoleJpaEntity asignacion(Long roleId)
            throws ReflectiveOperationException {
        RoleJpaEntity role = ReflectionEntities.newInstance(RoleJpaEntity.class);
        role.setId(roleId);
        EmployeeRoleJpaEntity entity = new EmployeeRoleJpaEntity();
        entity.setRole(role);
        return entity;
    }

    private static RolePermissionJpaEntity concesion(String codigo)
            throws ReflectiveOperationException {
        PermissionJpaEntity permission = ReflectionEntities.newInstance(PermissionJpaEntity.class);
        permission.setCode(codigo);
        RolePermissionJpaEntity entity = new RolePermissionJpaEntity();
        entity.setPermission(permission);
        return entity;
    }

    @Nested
    @DisplayName("resolveFor")
    class Resolver {

        @Test
        @DisplayName("agrega los códigos de permiso de todos los roles del empleado")
        void agrega_los_codigos_de_todos_los_roles() throws Exception {
            when(employeeRoleJpaRepository.findByEmployeeId(7L))
                    .thenReturn(List.of(asignacion(1L), asignacion(2L)));
            when(rolePermissionJpaRepository.findByRoleIdIn(List.of(1L, 2L)))
                    .thenReturn(List.of(concesion("animal.read"), concesion("animal.update")));

            assertThat(resolver.resolveFor(7L)).containsExactlyInAnyOrder("animal.read",
                    "animal.update");
        }

        @Test
        @DisplayName("un empleado sin roles no consulta permisos y devuelve vacío")
        void sin_roles_no_consulta_permisos() {
            when(employeeRoleJpaRepository.findByEmployeeId(7L)).thenReturn(List.of());

            assertThat(resolver.resolveFor(7L)).isEmpty();
            org.mockito.Mockito.verifyNoInteractions(rolePermissionJpaRepository);
        }
    }

    @Test
    @DisplayName("evict no lanza: su efecto es la anotación @CacheEvict, no el cuerpo del método")
    void evict_no_lanza() {
        resolver.evict(7L);
    }
}
