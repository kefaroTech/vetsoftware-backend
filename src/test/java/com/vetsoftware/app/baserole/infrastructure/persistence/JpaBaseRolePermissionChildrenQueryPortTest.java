package com.vetsoftware.app.baserole.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBaseRolePermissionChildrenQueryPort")
class JpaBaseRolePermissionChildrenQueryPortTest {

    @Mock
    private BaseRolePermissionJpaRepository jpaRepository;

    @InjectMocks
    private JpaBaseRolePermissionChildrenQueryPort queryPort;

    @Test
    @DisplayName("delega en existsByBaseRole_Id")
    void delega_en_exists_by_base_role_id() {
        when(jpaRepository.existsByBaseRole_Id(1L)).thenReturn(true);

        assertThat(queryPort.existsActiveByBaseRoleId(1L)).isTrue();
    }

    @Test
    @DisplayName("sin hijos activos devuelve false")
    void sin_hijos_activos_devuelve_false() {
        when(jpaRepository.existsByBaseRole_Id(2L)).thenReturn(false);

        assertThat(queryPort.existsActiveByBaseRoleId(2L)).isFalse();
    }
}
