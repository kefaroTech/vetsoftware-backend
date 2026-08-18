package com.vetsoftware.app.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.testsupport.ReflectionEntities;
import com.vetsoftware.app.systempermission.infrastructure.persistence.SystemPermissionJpaEntity;
import com.vetsoftware.app.systemuserpermission.infrastructure.persistence.SystemUserPermissionJpaEntity;
import com.vetsoftware.app.systemuserpermission.infrastructure.persistence.SystemUserPermissionJpaRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaSystemPermissionResolverTest {

    @Mock
    private SystemUserPermissionJpaRepository systemUserPermissionJpaRepository;
    @InjectMocks
    private JpaSystemPermissionResolver resolver;

    private static SystemUserPermissionJpaEntity concesion(String codigo)
            throws ReflectiveOperationException {
        SystemPermissionJpaEntity permission = ReflectionEntities
                .newInstance(SystemPermissionJpaEntity.class);
        permission.setCode(codigo);
        SystemUserPermissionJpaEntity entity = new SystemUserPermissionJpaEntity();
        entity.setSystemPermission(permission);
        return entity;
    }

    @Nested
    @DisplayName("resolveFor")
    class Resolver {

        @Test
        @DisplayName("mapea los códigos de permiso concedidos al usuario de sistema")
        void mapea_los_codigos_concedidos() throws Exception {
            when(systemUserPermissionJpaRepository.findBySystemUserId(2L))
                    .thenReturn(List.of(concesion("company.create"), concesion("company.delete")));

            assertThat(resolver.resolveFor(2L)).containsExactlyInAnyOrder("company.create",
                    "company.delete");
        }

        @Test
        @DisplayName("un usuario sin permisos concedidos devuelve vacío")
        void sin_permisos_devuelve_vacio() {
            when(systemUserPermissionJpaRepository.findBySystemUserId(2L)).thenReturn(List.of());

            assertThat(resolver.resolveFor(2L)).isEmpty();
        }
    }

    @Test
    @DisplayName("evict no lanza: su efecto es la anotación @CacheEvict")
    void evict_no_lanza() {
        resolver.evict(2L);
    }
}
