package com.vetsoftware.app.systemuser.domain;

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
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SystemUser — invariantes y ciclo de vida del usuario de plataforma")
class SystemUserTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir seis
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 1L;
        private String code = "svc-integracion";
        private String hashPassword = "hash-almacenado";
        private LocalDateTime createdDate = CREADO;
        private boolean enabled = true;
        private Long authVersion = 0L;

        private Builder code(String v) {
            this.code = v;
            return this;
        }

        private Builder hashPassword(String v) {
            this.hashPassword = v;
            return this;
        }

        private Builder authVersion(Long v) {
            this.authVersion = v;
            return this;
        }

        private SystemUser build() {
            return new SystemUser(id, code, hashPassword, createdDate, null, enabled, authVersion);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            SystemUser systemUser = valido().build();

            assertThat(systemUser.getId()).isEqualTo(1L);
            assertThat(systemUser.getCode()).isEqualTo("svc-integracion");
            assertThat(systemUser.getHashPassword()).isEqualTo("hash-almacenado");
            assertThat(systemUser.getCreatedDate()).isEqualTo(CREADO);
            assertThat(systemUser.isEnabled()).isTrue();
            assertThat(systemUser.getAuthVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("authVersion null colapsa a cero")
        void auth_version_null_colapsa_a_cero() {
            SystemUser systemUser = valido().authVersion(null).build();

            assertThat(systemUser.getAuthVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("authVersion no nulo se conserva tal cual")
        void auth_version_no_nulo_se_conserva() {
            SystemUser systemUser = valido().authVersion(7L).build();

            assertThat(systemUser.getAuthVersion()).isEqualTo(7L);
        }

        @Test
        @DisplayName("create() nace sin id, habilitado, con authVersion cero")
        void create_nace_sin_id_habilitado_con_auth_version_cero() {
            SystemUser systemUser = SystemUser.create("svc-integracion", "hash-almacenado");

            assertThat(systemUser.getId()).isNull();
            assertThat(systemUser.isEnabled()).isTrue();
            assertThat(systemUser.getAuthVersion()).isEqualTo(0L);
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Mismo patron de
            // deuda que Animal.create documentado en "Determinismo" del CLAUDE.md;
            // SystemUser no esta en esa lista todavia.
            assertThat(systemUser.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("code de 50 chars se acepta")
        void code_de_50_chars_se_acepta() {
            assertThatCode(() -> valido().code("x".repeat(50)).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
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
                    arguments("password null",
                            (ThrowingCallable) () -> valido().hashPassword(null).build(),
                            "password is required"),
                    arguments("password vacio",
                            (ThrowingCallable) () -> valido().hashPassword("").build(),
                            "password is required"),
                    arguments("password en blanco",
                            (ThrowingCallable) () -> valido().hashPassword("   ").build(),
                            "password is required"));
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
        @DisplayName("reemplaza el code y conserva el resto del agregado")
        void reemplaza_el_code_y_conserva_el_resto() {
            SystemUser systemUser = valido().build();

            systemUser.update("svc-nuevo");

            assertThat(systemUser.getCode()).isEqualTo("svc-nuevo");
            assertThat(systemUser.getId()).isEqualTo(1L);
            assertThat(systemUser.getHashPassword()).isEqualTo("hash-almacenado");
            assertThat(systemUser.getCreatedDate()).isEqualTo(CREADO);
        }

        @ParameterizedTest(name = "code \"{0}\"")
        @ValueSource(strings = {"", "   "})
        @DisplayName("rechaza un code vacio o en blanco y no toca el agregado")
        void rechaza_un_code_vacio_o_en_blanco(String codeInvalido) {
            SystemUser systemUser = valido().build();

            assertThatThrownBy(() -> systemUser.update(codeInvalido))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code is required");

            assertThat(systemUser.getCode()).isEqualTo("svc-integracion");
        }

        @Test
        @DisplayName("rechaza un code null")
        void rechaza_un_code_null() {
            SystemUser systemUser = valido().build();

            assertThatThrownBy(() -> systemUser.update(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code is required");
        }

        @Test
        @DisplayName("rechaza un code de mas de 50 chars y no toca el agregado")
        void rechaza_un_code_de_mas_de_50_chars() {
            SystemUser systemUser = valido().build();

            assertThatThrownBy(() -> systemUser.update("x".repeat(51)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code must be 50 chars or less");

            assertThat(systemUser.getCode()).isEqualTo("svc-integracion");
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            SystemUser systemUser = valido().build();
            systemUser.disable();

            systemUser.update("svc-nuevo");

            assertThat(systemUser.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            SystemUser systemUser = valido().build();

            systemUser.disable();
            assertThat(systemUser.isEnabled()).isFalse();
            systemUser.disable();
            assertThat(systemUser.isEnabled()).isFalse();

            systemUser.enable();
            assertThat(systemUser.isEnabled()).isTrue();
            systemUser.enable();
            assertThat(systemUser.isEnabled()).isTrue();
        }
    }
}
