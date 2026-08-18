package com.vetsoftware.app.role.domain;

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

@DisplayName("Role — invariantes y ciclo de vida del agregado")
class RoleTest {

    private static final CompanyRef CLINICA = new CompanyRef(9L, "Clinica Norte", "NIT-900");
    private static final CompanyRef OTRA_CLINICA = new CompanyRef(10L, "Clinica Sur", "NIT-901");

    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 1L;
        private String name = "Veterinario";
        private String code = "VET";
        private CompanyRef company = CLINICA;
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

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private Role build() {
            return new Role(id, name, code, company, createdDate, enabled);
        }

        private void applyTo(Role role) {
            role.update(name, code, company);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Role role = valido().build();

            assertThat(role.getId()).isEqualTo(1L);
            assertThat(role.getName()).isEqualTo("Veterinario");
            assertThat(role.getCode()).isEqualTo("VET");
            assertThat(role.getCompany()).isEqualTo(CLINICA);
            assertThat(role.getCreatedDate()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 30));
            assertThat(role.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y con la fecha actual")
        void create_nace_sin_id_y_habilitado() {
            Role role = Role.create("Veterinario", "VET", CLINICA);

            assertThat(role.getId()).isNull();
            assertThat(role.isEnabled()).isTrue();
            assertThat(role.getCompany()).isEqualTo(CLINICA);
            assertThat(role.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas en la construccion")
    class InvariantesConstruccion {

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
                    arguments("company null",
                            (ThrowingCallable) () -> valido().company(null).build(),
                            "company is required"));
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
    }

    @Nested
    @DisplayName("update")
    class Update {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null",
                            (java.util.function.Consumer<Role>) r -> valido().name(null).applyTo(r),
                            "name is required"),
                    arguments("code null",
                            (java.util.function.Consumer<Role>) r -> valido().code(null).applyTo(r),
                            "code is required"),
                    arguments("company null", (java.util.function.Consumer<Role>) r -> valido()
                            .company(null).applyTo(r), "company is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("rechaza las mismas invariantes que el constructor")
        void rechaza_las_mismas_invariantes(String caso, java.util.function.Consumer<Role> mutacion,
                String mensaje) {
            Role role = valido().build();

            assertThatThrownBy(() -> mutacion.accept(role))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id y createdDate")
        void reemplaza_los_campos_mutables_y_conserva_id_y_created_date() {
            Role role = valido().build();

            valido().name("Administrador").code("ADMIN").company(OTRA_CLINICA).applyTo(role);

            assertThat(role.getName()).isEqualTo("Administrador");
            assertThat(role.getCode()).isEqualTo("ADMIN");
            assertThat(role.getCompany()).isEqualTo(OTRA_CLINICA);
            assertThat(role.getId()).isEqualTo(1L);
            assertThat(role.getCreatedDate()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 30));
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            Role role = valido().build();

            assertThatThrownBy(() -> valido().name("Administrador").code(null).applyTo(role))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(role.getName()).isEqualTo("Veterinario");
            assertThat(role.getCode()).isEqualTo("VET");
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            Role role = valido().build();
            role.disable();

            valido().name("Administrador").applyTo(role);

            assertThat(role.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            Role role = valido().build();

            role.disable();
            assertThat(role.isEnabled()).isFalse();
            role.disable();
            assertThat(role.isEnabled()).isFalse();

            role.enable();
            assertThat(role.isEnabled()).isTrue();
            role.enable();
            assertThat(role.isEnabled()).isTrue();
        }
    }
}
