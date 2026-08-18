package com.vetsoftware.app.basepermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBaseRolePermissionChildrenQueryPort — baseRolePermission activos de un permiso base")
class JpaBaseRolePermissionChildrenQueryPortTest {

    @Mock
    private BaseRolePermissionJpaRepository jpaRepository;
    @InjectMocks
    private JpaBaseRolePermissionChildrenQueryPort port;

    @Nested
    @DisplayName("consulta")
    class Consulta {

        @Test
        @DisplayName("delega en existsByBasePermission_Id y devuelve true si hay hijos activos")
        void delega_y_devuelve_true_si_hay_hijos_activos() {
            when(jpaRepository.existsByBasePermission_Id(7L)).thenReturn(true);

            assertThat(port.existsActiveByBasePermissionId(7L)).isTrue();
        }

        @Test
        @DisplayName("devuelve false si no hay baseRolePermission activos de ese permiso")
        void devuelve_false_si_no_hay_hijos_activos() {
            when(jpaRepository.existsByBasePermission_Id(7L)).thenReturn(false);

            assertThat(port.existsActiveByBasePermissionId(7L)).isFalse();
        }
    }
}
