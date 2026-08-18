package com.vetsoftware.app.systempermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systemuserpermission.infrastructure.persistence.SystemUserPermissionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSystemUserPermissionChildrenQueryPort")
class JpaSystemUserPermissionChildrenQueryPortTest {

    @Mock
    private SystemUserPermissionJpaRepository jpaRepository;

    private JpaSystemUserPermissionChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaSystemUserPermissionChildrenQueryPort(jpaRepository);
    }

    @Nested
    @DisplayName("existsActiveBySystemPermissionId")
    class ExistsActiveBySystemPermissionId {

        @Test
        @DisplayName("delega en existsBySystemPermission_Id del repositorio Spring Data")
        void delega_en_exists_by_system_permission_id() {
            when(jpaRepository.existsBySystemPermission_Id(1L)).thenReturn(true);

            assertThat(port.existsActiveBySystemPermissionId(1L)).isTrue();
        }

        @Test
        @DisplayName("sin usuarios de sistema activos devuelve false")
        void sin_hijos_activos_devuelve_false() {
            when(jpaRepository.existsBySystemPermission_Id(1L)).thenReturn(false);

            assertThat(port.existsActiveBySystemPermissionId(1L)).isFalse();
        }
    }
}
