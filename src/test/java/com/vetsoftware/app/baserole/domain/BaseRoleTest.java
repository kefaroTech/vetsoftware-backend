package com.vetsoftware.app.baserole.domain;

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

@DisplayName("BaseRole — invariantes y ciclo de vida del agregado")
class BaseRoleTest {

    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 1L;
        private String name = "Veterinario";
        private String code = "VET";
        private Boolean mandatory = false;
        private final LocalDateTime createdDate = LocalDateTime.of(2026, 1, 15, 10, 30);
        private boolean enabled = true;

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder code(String v) {
            this.code = v;
            return this;
        }

        private Builder mandatory(Boolean v) {
            this.mandatory = v;
            return this;
        }

        private BaseRole build() {
            return new BaseRole(id, name, code, mandatory, createdDate, enabled);
        }

        private void applyTo(BaseRole baseRole) {
            baseRole.update(name, code, mandatory);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            BaseRole baseRole = valido().build();

            assertThat(baseRole.getId()).isEqualTo(1L);
            assertThat(baseRole.getName()).isEqualTo("Veterinario");
            assertThat(baseRole.getCode()).isEqualTo("VET");
            assertThat(baseRole.getMandatory()).isFalse();
            assertThat(baseRole.getCreatedDate()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 30));
            assertThat(baseRole.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id y habilitado")
        void create_nace_sin_id_y_habilitado() {
            BaseRole baseRole = BaseRole.create("Veterinario", "VET", true);

            assertThat(baseRole.getId()).isNull();
            assertThat(baseRole.isEnabled()).isTrue();
            assertThat(baseRole.getMandatory()).isTrue();
            assertThat(baseRole.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null", (ThrowingCallable) () -> valido().name(null).build(),
                            "name is required"),
                    arguments("name vacio", (ThrowingCallable) () -> valido().name("").build(),
                            "name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> valido().name("   ").build(),
                            "name is required"),
                    arguments("name de 101 chars",
                            (ThrowingCallable) () -> valido().name("x".repeat(101)).build(),
                            "name must be 100 chars or less"),
                    arguments("code null", (ThrowingCallable) () -> valido().code(null).build(),
                            "code is required"),
                    arguments("code vacio", (ThrowingCallable) () -> valido().code("").build(),
                            "code is required"),
                    arguments("code en blanco",
                            (ThrowingCallable) () -> valido().code("   ").build(),
                            "code is required"),
                    arguments("code de 51 chars",
                            (ThrowingCallable) () -> valido().code("x".repeat(51)).build(),
                            "code must be 50 chars or less"),
                    arguments("mandatory null",
                            (ThrowingCallable) () -> valido().mandatory(null).build(),
                            "mandatory is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("name de 100 chars se acepta")
        void name_de_100_chars_se_acepta() {
            assertThatCode(() -> valido().name("x".repeat(100)).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("code de 50 chars se acepta")
        void code_de_50_chars_se_acepta() {
            assertThatCode(() -> valido().code("x".repeat(50)).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("mandatory false es valido — no todo rol base es obligatorio")
        void mandatory_false_es_valido() {
            assertThatCode(() -> valido().mandatory(false).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id y createdDate")
        void reemplaza_los_campos_mutables_y_conserva_id_y_created_date() {
            BaseRole baseRole = valido().build();

            valido().name("Administrador").code("ADMIN").mandatory(true).applyTo(baseRole);

            assertThat(baseRole.getName()).isEqualTo("Administrador");
            assertThat(baseRole.getCode()).isEqualTo("ADMIN");
            assertThat(baseRole.getMandatory()).isTrue();
            assertThat(baseRole.getId()).isEqualTo(1L);
            assertThat(baseRole.getCreatedDate()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 30));
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            BaseRole baseRole = valido().build();

            assertThatThrownBy(() -> valido().name("Administrador").code(null).applyTo(baseRole))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(baseRole.getName()).isEqualTo("Veterinario");
            assertThat(baseRole.getCode()).isEqualTo("VET");
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            BaseRole baseRole = valido().build();
            baseRole.disable();

            valido().name("Administrador").applyTo(baseRole);

            assertThat(baseRole.isEnabled()).isFalse();
        }

        static Stream<Arguments> casosInvalidosUpdate() {
            return Stream.of(
                    arguments("name null",
                            (ThrowingCallable) () -> valido().name(null).applyTo(valido().build()),
                            "name is required"),
                    arguments("name vacio",
                            (ThrowingCallable) () -> valido().name("").applyTo(valido().build()),
                            "name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> valido().name("   ").applyTo(valido().build()),
                            "name is required"),
                    arguments("name de 101 chars",
                            (ThrowingCallable) () -> valido().name("x".repeat(101))
                                    .applyTo(valido().build()),
                            "name must be 100 chars or less"),
                    arguments("code null",
                            (ThrowingCallable) () -> valido().code(null).applyTo(valido().build()),
                            "code is required"),
                    arguments("code vacio",
                            (ThrowingCallable) () -> valido().code("").applyTo(valido().build()),
                            "code is required"),
                    arguments("code en blanco",
                            (ThrowingCallable) () -> valido().code("   ").applyTo(valido().build()),
                            "code is required"),
                    arguments("code de 51 chars",
                            (ThrowingCallable) () -> valido().code("x".repeat(51)).applyTo(
                                    valido().build()),
                            "code must be 50 chars or less"),
                    arguments("mandatory null", (ThrowingCallable) () -> valido().mandatory(null)
                            .applyTo(valido().build()), "mandatory is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidosUpdate")
        @DisplayName("update rechaza las mismas invariantes que el constructor")
        void update_rechaza(String caso, ThrowingCallable actualizacion, String mensaje) {
            assertThatThrownBy(actualizacion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("update acepta name de 100 chars y code de 50 chars")
        void update_acepta_los_limites_de_longitud() {
            BaseRole baseRole = valido().build();

            assertThatCode(
                    () -> valido().name("x".repeat(100)).code("x".repeat(50)).applyTo(baseRole))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            BaseRole baseRole = valido().build();

            baseRole.disable();
            assertThat(baseRole.isEnabled()).isFalse();
            baseRole.disable();
            assertThat(baseRole.isEnabled()).isFalse();

            baseRole.enable();
            assertThat(baseRole.isEnabled()).isTrue();
            baseRole.enable();
            assertThat(baseRole.isEnabled()).isTrue();
        }
    }
}
