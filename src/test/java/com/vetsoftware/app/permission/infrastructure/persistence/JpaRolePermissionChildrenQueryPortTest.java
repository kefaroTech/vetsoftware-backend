package com.vetsoftware.app.permission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaRolePermissionChildrenQueryPort")
class JpaRolePermissionChildrenQueryPortTest {

    @Mock
    private RolePermissionJpaRepository jpaRepository;

    @InjectMocks
    private JpaRolePermissionChildrenQueryPort port;

    @Test
    @DisplayName("delega en existsByPermission_Id")
    void delega_en_exists_by_permission_id() {
        when(jpaRepository.existsByPermission_Id(7L)).thenReturn(true);

        assertThat(port.existsActiveByPermissionId(7L)).isTrue();
    }

    @Test
    @DisplayName("sin role-permissions activos devuelve false")
    void sin_role_permissions_activos_devuelve_false() {
        when(jpaRepository.existsByPermission_Id(7L)).thenReturn(false);

        assertThat(port.existsActiveByPermissionId(7L)).isFalse();
    }
}
