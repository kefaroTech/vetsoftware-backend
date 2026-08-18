package com.vetsoftware.app.systemuserpermission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.systemuserpermission.testsupport.SystemUserPermissionMother;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SystemUserPermission — invariantes y ciclo de vida")
class SystemUserPermissionTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);

            SystemUserPermission sup = new SystemUserPermission(1L,
                    SystemUserPermissionMother.USUARIO, SystemUserPermissionMother.PERMISO, creado,
                    true);

            assertThat(sup.getId()).isEqualTo(1L);
            assertThat(sup.getSystemUser()).isEqualTo(SystemUserPermissionMother.USUARIO);
            assertThat(sup.getSystemPermission()).isEqualTo(SystemUserPermissionMother.PERMISO);
            assertThat(sup.getCreatedDate()).isEqualTo(creado);
            assertThat(sup.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("rechaza systemUser nulo")
        void rechaza_system_user_nulo() {
            assertThatThrownBy(() -> new SystemUserPermission(1L, null,
                    SystemUserPermissionMother.PERMISO, LocalDateTime.now(), true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("systemUser is required");
        }

        @Test
        @DisplayName("rechaza systemPermission nulo")
        void rechaza_system_permission_nulo() {
            assertThatThrownBy(() -> new SystemUserPermission(1L,
                    SystemUserPermissionMother.USUARIO, null, LocalDateTime.now(), true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("systemPermission is required");
        }

        @Test
        @DisplayName("create() nace sin id y habilitado")
        void create_nace_sin_id_y_habilitado() {
            SystemUserPermission sup = SystemUserPermission
                    .create(SystemUserPermissionMother.USUARIO, SystemUserPermissionMother.PERMISO);

            assertThat(sup.getId()).isNull();
            assertThat(sup.isEnabled()).isTrue();
            assertThat(sup.getSystemUser()).isEqualTo(SystemUserPermissionMother.USUARIO);
            assertThat(sup.getSystemPermission()).isEqualTo(SystemUserPermissionMother.PERMISO);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza usuario y permiso, conserva id y createdDate")
        void reemplaza_usuario_y_permiso_conserva_id_y_created_date() {
            SystemUserPermission sup = SystemUserPermissionMother.asignacionActiva();

            sup.update(SystemUserPermissionMother.OTRO_USUARIO,
                    SystemUserPermissionMother.OTRO_PERMISO);

            assertThat(sup.getSystemUser()).isEqualTo(SystemUserPermissionMother.OTRO_USUARIO);
            assertThat(sup.getSystemPermission())
                    .isEqualTo(SystemUserPermissionMother.OTRO_PERMISO);
            assertThat(sup.getId()).isEqualTo(SystemUserPermissionMother.ID);
            assertThat(sup.getCreatedDate()).isEqualTo(SystemUserPermissionMother.CREADO);
        }

        @Test
        @DisplayName("rechaza actualizar a systemUser nulo sin tocar el estado previo")
        void rechaza_actualizar_a_system_user_nulo() {
            SystemUserPermission sup = SystemUserPermissionMother.asignacionActiva();

            assertThatThrownBy(() -> sup.update(null, SystemUserPermissionMother.OTRO_PERMISO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("systemUser is required");

            assertThat(sup.getSystemUser()).isEqualTo(SystemUserPermissionMother.USUARIO);
        }

        @Test
        @DisplayName("rechaza actualizar a systemPermission nulo sin tocar el estado previo")
        void rechaza_actualizar_a_system_permission_nulo() {
            SystemUserPermission sup = SystemUserPermissionMother.asignacionActiva();

            assertThatThrownBy(() -> sup.update(SystemUserPermissionMother.OTRO_USUARIO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("systemPermission is required");

            assertThat(sup.getSystemPermission()).isEqualTo(SystemUserPermissionMother.PERMISO);
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("enable y disable alternan el estado y son idempotentes")
        void enable_y_disable_alternan_el_estado_y_son_idempotentes() {
            SystemUserPermission sup = SystemUserPermissionMother.asignacionActiva();

            sup.disable();
            assertThat(sup.isEnabled()).isFalse();
            sup.disable();
            assertThat(sup.isEnabled()).isFalse();

            sup.enable();
            assertThat(sup.isEnabled()).isTrue();
            sup.enable();
            assertThat(sup.isEnabled()).isTrue();
        }
    }
}
