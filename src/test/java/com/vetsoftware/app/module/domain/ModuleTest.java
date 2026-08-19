package com.vetsoftware.app.module.domain;

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

@DisplayName("Module — entidad de dominio")
class ModuleTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Module module = new Module(1L, "Inventario", "INV", CREADO, null, true);

            assertThat(module.getId()).isEqualTo(1L);
            assertThat(module.getName()).isEqualTo("Inventario");
            assertThat(module.getCode()).isEqualTo("INV");
            assertThat(module.getCreatedDate()).isEqualTo(CREADO);
            assertThat(module.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y con la fecha de hoy")
        void create_nace_sin_id_habilitado_y_con_la_fecha_de_hoy() {
            Module module = Module.create("Inventario", "INV");

            assertThat(module.getId()).isNull();
            assertThat(module.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada
            // en "Determinismo" del CLAUDE.md.
            assertThat(module.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("name de 100 chars y code de 50 chars exactos se aceptan")
        void name_y_code_en_el_limite_se_aceptan() {
            ThrowingCallable enElLimite = () -> new Module(1L, "x".repeat(100), "y".repeat(50),
                    CREADO, null, true);

            assertThatCode(enElLimite).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas en el constructor")
    class InvariantesConstructor {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null",
                            (ThrowingCallable) () -> new Module(1L, null, "INV", CREADO, null,
                                    true),
                            "name is required"),
                    arguments("name vacio",
                            (ThrowingCallable) () -> new Module(1L, "", "INV", CREADO, null, true),
                            "name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> new Module(1L, "   ", "INV", CREADO, null,
                                    true),
                            "name is required"),
                    arguments("name de 101 chars",
                            (ThrowingCallable) () -> new Module(1L, "x".repeat(101), "INV", CREADO,
                                    null, true),
                            "name must be 100 chars or less"),
                    arguments("code null",
                            (ThrowingCallable) () -> new Module(1L, "Inventario", null, CREADO,
                                    null, true),
                            "code is required"),
                    arguments("code vacio",
                            (ThrowingCallable) () -> new Module(1L, "Inventario", "", CREADO, null,
                                    true),
                            "code is required"),
                    arguments("code en blanco",
                            (ThrowingCallable) () -> new Module(1L, "Inventario", "   ", CREADO,
                                    null, true),
                            "code is required"),
                    arguments(
                            "code de 51 chars", (ThrowingCallable) () -> new Module(1L,
                                    "Inventario", "x".repeat(51), CREADO, null, true),
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
            Module module = new Module(1L, "Inventario", "INV", CREADO, null, true);

            module.update("Caja", "CAJA");

            assertThat(module.getName()).isEqualTo("Caja");
            assertThat(module.getCode()).isEqualTo("CAJA");
            assertThat(module.getId()).isEqualTo(1L);
            assertThat(module.getCreatedDate()).isEqualTo(CREADO);
        }

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null",
                            (ThrowingCallable) () -> new Module(1L, "Inventario", "INV", CREADO,
                                    null, true).update(null, "CAJA"),
                            "name is required"),
                    arguments("name de 101 chars",
                            (ThrowingCallable) () -> new Module(1L, "Inventario", "INV", CREADO,
                                    null, true).update("x".repeat(101), "CAJA"),
                            "name must be 100 chars or less"),
                    arguments("code null",
                            (ThrowingCallable) () -> new Module(1L, "Inventario", "INV", CREADO,
                                    null, true).update("Caja", null),
                            "code is required"),
                    arguments("code de 51 chars",
                            (ThrowingCallable) () -> new Module(1L, "Inventario", "INV", CREADO,
                                    null, true).update("Caja", "x".repeat(51)),
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
            Module module = new Module(1L, "Inventario", "INV", CREADO, null, true);

            assertThatThrownBy(() -> module.update("Caja", null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(module.getName()).isEqualTo("Inventario");
            assertThat(module.getCode()).isEqualTo("INV");
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            Module module = new Module(1L, "Inventario", "INV", CREADO, null, true);

            module.disable();
            assertThat(module.isEnabled()).isFalse();
            module.disable();
            assertThat(module.isEnabled()).isFalse();

            module.enable();
            assertThat(module.isEnabled()).isTrue();
            module.enable();
            assertThat(module.isEnabled()).isTrue();
        }
    }
}
