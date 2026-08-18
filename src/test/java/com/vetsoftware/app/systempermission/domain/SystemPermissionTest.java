package com.vetsoftware.app.systempermission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("SystemPermission — entidad de dominio")
class SystemPermissionTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            SystemPermission permission = new SystemPermission(1L, "Administrar usuarios",
                    "admin.users", CREADO, true);

            assertThat(permission.getId()).isEqualTo(1L);
            assertThat(permission.getName()).isEqualTo("Administrar usuarios");
            assertThat(permission.getCode()).isEqualTo("admin.users");
            assertThat(permission.getCreatedDate()).isEqualTo(CREADO);
            assertThat(permission.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y con la fecha de hoy")
        void create_nace_sin_id_habilitado_y_con_la_fecha_de_hoy() {
            SystemPermission permission = SystemPermission.create("Administrar usuarios",
                    "admin.users");

            assertThat(permission.getId()).isNull();
            assertThat(permission.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada
            // en "Determinismo" del CLAUDE.md.
            assertThat(permission.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("name de 100 chars y code de 50 chars exactos se aceptan")
        void name_y_code_en_el_limite_se_aceptan() {
            assertThatCode(
                    () -> new SystemPermission(1L, "x".repeat(100), "y".repeat(50), CREADO, true))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas en el constructor")
    class InvariantesConstructor {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null",
                            (ThrowingCallable) () -> new SystemPermission(1L, null, "admin.users",
                                    CREADO, true),
                            "name is required"),
                    arguments("name vacio",
                            (ThrowingCallable) () -> new SystemPermission(1L, "", "admin.users",
                                    CREADO, true),
                            "name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> new SystemPermission(1L, "   ", "admin.users",
                                    CREADO, true),
                            "name is required"),
                    arguments("name de 101 chars",
                            (ThrowingCallable) () -> new SystemPermission(1L, "x".repeat(101),
                                    "admin.users", CREADO, true),
                            "name must be 100 chars or less"),
                    arguments("code null",
                            (ThrowingCallable) () -> new SystemPermission(1L, "Administrar", null,
                                    CREADO, true),
                            "code is required"),
                    arguments("code vacio",
                            (ThrowingCallable) () -> new SystemPermission(1L, "Administrar", "",
                                    CREADO, true),
                            "code is required"),
                    arguments("code en blanco",
                            (ThrowingCallable) () -> new SystemPermission(1L, "Administrar", "   ",
                                    CREADO, true),
                            "code is required"),
                    arguments("code de 51 chars",
                            (ThrowingCallable) () -> new SystemPermission(1L, "Administrar",
                                    "x".repeat(51), CREADO, true),
                            "code must be 50 chars or less"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza name y code y conserva id, createdDate y enabled")
        void reemplaza_name_y_code_y_conserva_el_resto() {
            SystemPermission permission = new SystemPermission(1L, "Administrar usuarios",
                    "admin.users", CREADO, true);

            permission.update("Administrar roles", "admin.roles");

            assertThat(permission.getName()).isEqualTo("Administrar roles");
            assertThat(permission.getCode()).isEqualTo("admin.roles");
            assertThat(permission.getId()).isEqualTo(1L);
            assertThat(permission.getCreatedDate()).isEqualTo(CREADO);
        }

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null",
                            (ThrowingCallable) () -> new SystemPermission(1L, "Administrar",
                                    "admin.users", CREADO, true).update(null, "admin.roles"),
                            "name is required"),
                    arguments("name de 101 chars",
                            (ThrowingCallable) () -> new SystemPermission(1L, "Administrar",
                                    "admin.users", CREADO, true)
                                    .update("x".repeat(101), "admin.roles"),
                            "name must be 100 chars or less"),
                    arguments("code null",
                            (ThrowingCallable) () -> new SystemPermission(1L, "Administrar",
                                    "admin.users", CREADO, true).update("Administrar roles", null),
                            "code is required"),
                    arguments("code de 51 chars",
                            (ThrowingCallable) () -> new SystemPermission(1L, "Administrar",
                                    "admin.users", CREADO, true)
                                    .update("Administrar roles", "x".repeat(51)),
                            "code must be 50 chars or less"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("update rechaza los mismos invariantes que el constructor")
        void update_rechaza(String caso, ThrowingCallable actualizacion, String mensaje) {
            assertThatThrownBy(actualizacion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            SystemPermission permission = new SystemPermission(1L, "Administrar usuarios",
                    "admin.users", CREADO, true);

            assertThatThrownBy(() -> permission.update("Administrar roles", null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(permission.getName()).isEqualTo("Administrar usuarios");
            assertThat(permission.getCode()).isEqualTo("admin.users");
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            SystemPermission permission = new SystemPermission(1L, "Administrar usuarios",
                    "admin.users", CREADO, true);

            permission.disable();
            assertThat(permission.isEnabled()).isFalse();
            permission.disable();
            assertThat(permission.isEnabled()).isFalse();

            permission.enable();
            assertThat(permission.isEnabled()).isTrue();
            permission.enable();
            assertThat(permission.isEnabled()).isTrue();
        }
    }
}
