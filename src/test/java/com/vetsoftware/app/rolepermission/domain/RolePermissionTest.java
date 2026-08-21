package com.vetsoftware.app.rolepermission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.rolepermission.testsupport.RolePermissionMother;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RolePermission — entidad de dominio")
class RolePermissionTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("conserva todos los datos que recibe")
        void conserva_todos_los_datos() {
            LocalDateTime creado = LocalDateTime.of(2026, 3, 1, 8, 0);

            RolePermission rp = new RolePermission(1L, RolePermissionMother.VETERINARIO,
                    RolePermissionMother.VER_ANIMALES, creado, true);

            assertThat(rp.getId()).isEqualTo(1L);
            assertThat(rp.getRole()).isEqualTo(RolePermissionMother.VETERINARIO);
            assertThat(rp.getPermission()).isEqualTo(RolePermissionMother.VER_ANIMALES);
            assertThat(rp.getCreatedDate()).isEqualTo(creado);
            assertThat(rp.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("rechaza un rol nulo")
        void rechaza_un_rol_nulo() {
            assertThatThrownBy(() -> new RolePermission(1L, null, RolePermissionMother.VER_ANIMALES,
                    RolePermissionMother.CREADO, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("role is required");
        }

        @Test
        @DisplayName("rechaza un permiso nulo")
        void rechaza_un_permiso_nulo() {
            assertThatThrownBy(() -> new RolePermission(1L, RolePermissionMother.VETERINARIO, null,
                    RolePermissionMother.CREADO, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("permission is required");
        }

        @Test
        @DisplayName("admite id nulo porque la asignacion aun no se ha persistido")
        void admite_id_nulo() {
            RolePermission rp = new RolePermission(null, RolePermissionMother.VETERINARIO,
                    RolePermissionMother.VER_ANIMALES, RolePermissionMother.CREADO, true);

            assertThat(rp.getId()).isNull();
        }

        @Test
        @DisplayName("admite construirse deshabilitada para reflejar una fila desactivada")
        void admite_construirse_deshabilitada() {
            assertThat(RolePermissionMother.desactivada().isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("factoria create")
    class Creacion {

        @Test
        @DisplayName("nace sin id, habilitada y con la fecha de creacion informada")
        void nace_sin_id_y_habilitada() {
            RolePermission rp = RolePermission.create(RolePermissionMother.VETERINARIO,
                    RolePermissionMother.VER_ANIMALES);

            assertThat(rp.getId()).isNull();
            assertThat(rp.isEnabled()).isTrue();
            assertThat(rp.getCreatedDate()).isNotNull();
            assertThat(rp.getRole()).isEqualTo(RolePermissionMother.VETERINARIO);
            assertThat(rp.getPermission()).isEqualTo(RolePermissionMother.VER_ANIMALES);
        }

        @Test
        @DisplayName("propaga la validacion del rol nulo")
        void propaga_la_validacion_del_rol_nulo() {
            assertThatThrownBy(() -> RolePermission.create(null, RolePermissionMother.VER_ANIMALES))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("role is required");
        }

        @Test
        @DisplayName("propaga la validacion del permiso nulo")
        void propaga_la_validacion_del_permiso_nulo() {
            assertThatThrownBy(() -> RolePermission.create(RolePermissionMother.VETERINARIO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("permission is required");
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable apaga la asignacion")
        void disable_apaga_la_asignacion() {
            RolePermission rp = RolePermissionMother.activa();

            rp.disable();

            assertThat(rp.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("enable vuelve a encenderla")
        void enable_vuelve_a_encenderla() {
            RolePermission rp = RolePermissionMother.desactivada();

            rp.enable();

            assertThat(rp.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("enable sobre una activa es idempotente")
        void enable_sobre_una_activa_es_idempotente() {
            RolePermission rp = RolePermissionMother.activa();

            rp.enable();
            rp.enable();

            assertThat(rp.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("disable sobre una desactivada es idempotente")
        void disable_sobre_una_desactivada_es_idempotente() {
            RolePermission rp = RolePermissionMother.desactivada();

            rp.disable();
            rp.disable();

            assertThat(rp.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("excepcion de no encontrado")
    class NoEncontrado {

        @Test
        @DisplayName("el mensaje lleva el id que se busco")
        void el_mensaje_lleva_el_id() {
            assertThat(new RolePermissionNotFoundException(42L))
                    .hasMessageContaining("RolePermission not found: 42");
        }
    }
}
