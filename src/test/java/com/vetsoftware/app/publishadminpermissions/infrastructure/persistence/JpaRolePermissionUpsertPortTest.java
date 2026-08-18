package com.vetsoftware.app.publishadminpermissions.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaEntity;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaRepository;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaRepository;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaEntity;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code RoleJpaEntity} se mockea porque su constructor sin argumentos es
 * {@code protected}; {@code PermissionJpaEntity} tiene constructor publico pero
 * aqui solo actua como referencia devuelta por {@code getReferenceById}, asi
 * que se mockea igual por simetria — ninguna de las dos tiene logica propia.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaRolePermissionUpsertPort — vinculo idempotente rol-permiso")
class JpaRolePermissionUpsertPortTest {

    @Mock
    private RolePermissionJpaRepository rolePermissionJpaRepository;
    @Mock
    private RoleJpaRepository roleJpaRepository;
    @Mock
    private PermissionJpaRepository permissionJpaRepository;
    @Mock
    private RoleJpaEntity rolRef;
    @Mock
    private PermissionJpaEntity permisoRef;
    @InjectMocks
    private JpaRolePermissionUpsertPort port;

    @Nested
    @DisplayName("vinculo ya existente")
    class VinculoYaExistente {

        @Test
        @DisplayName("no crea un duplicado")
        void no_crea_un_duplicado() {
            when(rolePermissionJpaRepository.existsByRoleIdAndPermissionId(200L, 77L))
                    .thenReturn(true);

            boolean creado = port.linkIfAbsent(200L, 77L);

            assertThat(creado).isFalse();
            verify(rolePermissionJpaRepository, never()).save(any());
            verifyNoInteractions(roleJpaRepository, permissionJpaRepository);
        }
    }

    @Nested
    @DisplayName("vinculo nuevo")
    class VinculoNuevo {

        @Test
        @DisplayName("crea el vinculo entre el rol y el permiso")
        void crea_el_vinculo_entre_el_rol_y_el_permiso() {
            when(rolePermissionJpaRepository.existsByRoleIdAndPermissionId(200L, 77L))
                    .thenReturn(false);
            when(roleJpaRepository.getReferenceById(200L)).thenReturn(rolRef);
            when(permissionJpaRepository.getReferenceById(77L)).thenReturn(permisoRef);

            boolean creado = port.linkIfAbsent(200L, 77L);

            ArgumentCaptor<RolePermissionJpaEntity> captor = ArgumentCaptor
                    .forClass(RolePermissionJpaEntity.class);
            verify(rolePermissionJpaRepository).save(captor.capture());
            RolePermissionJpaEntity guardado = captor.getValue();
            assertThat(guardado.getRole()).isEqualTo(rolRef);
            assertThat(guardado.getPermission()).isEqualTo(permisoRef);
            assertThat(guardado.getCreatedDate()).isNotNull();
            assertThat(creado).isTrue();
        }
    }
}
