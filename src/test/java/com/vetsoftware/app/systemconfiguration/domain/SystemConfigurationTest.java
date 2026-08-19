package com.vetsoftware.app.systemconfiguration.domain;

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

@DisplayName("SystemConfiguration — invariantes de la configuracion global")
class SystemConfigurationTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private final Long id = 1L;
        private String propertyName = "uvt";
        private String value = "47065";
        private final LocalDateTime createdDate = CREADO;
        private final boolean enabled = true;

        private Builder propertyName(String v) {
            this.propertyName = v;
            return this;
        }

        private Builder value(String v) {
            this.value = v;
            return this;
        }

        private SystemConfiguration build() {
            return new SystemConfiguration(id, propertyName, value, createdDate, null, enabled);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            SystemConfiguration config = valido().build();

            assertThat(config.getId()).isEqualTo(1L);
            assertThat(config.getPropertyName()).isEqualTo("uvt");
            assertThat(config.getValue()).isEqualTo("47065");
            assertThat(config.getCreatedDate()).isEqualTo(CREADO);
            assertThat(config.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y con fecha actual")
        void create_nace_sin_id_habilitado_y_con_fecha_actual() {
            SystemConfiguration config = SystemConfiguration.create("uvt", "47065");

            assertThat(config.getId()).isNull();
            assertThat(config.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada
            // en "Determinismo" del CLAUDE.md.
            assertThat(config.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas por el constructor")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("propertyName null",
                            (ThrowingCallable) () -> valido().propertyName(null).build(),
                            "propertyName is required"),
                    arguments("propertyName vacio",
                            (ThrowingCallable) () -> valido().propertyName("").build(),
                            "propertyName is required"),
                    arguments("propertyName en blanco",
                            (ThrowingCallable) () -> valido().propertyName("   ").build(),
                            "propertyName is required"),
                    arguments("value null", (ThrowingCallable) () -> valido().value(null).build(),
                            "value is required"),
                    arguments("value vacio", (ThrowingCallable) () -> valido().value("").build(),
                            "value is required"),
                    arguments("value en blanco",
                            (ThrowingCallable) () -> valido().value("   ").build(),
                            "value is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("a diferencia de CompanySetting, aqui no hay tope de longitud")
        void no_hay_tope_de_longitud() {
            // SystemConfiguration.validate/validateValue solo comprueban null/blank: no
            // hay chequeo de longitud como en CompanySetting.propertyName/value.
            // Documentado aqui para que un cambio de invariante futuro lo note quien lea
            // el test, no solo el diff.
            assertThatCode(() -> valido().propertyName("x".repeat(500)).build())
                    .doesNotThrowAnyException();
            assertThatCode(() -> valido().value("x".repeat(500)).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza el valor y conserva el resto del agregado")
        void reemplaza_el_valor_y_conserva_el_resto() {
            SystemConfiguration config = valido().build();

            config.update("47100");

            assertThat(config.getValue()).isEqualTo("47100");
            assertThat(config.getId()).isEqualTo(1L);
            assertThat(config.getPropertyName()).isEqualTo("uvt");
            assertThat(config.getCreatedDate()).isEqualTo(CREADO);
        }

        @Test
        @DisplayName("rechaza un valor en blanco y no deja la configuracion a medias")
        void rechaza_un_valor_en_blanco() {
            SystemConfiguration config = valido().build();

            assertThatThrownBy(() -> config.update("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("value is required");

            assertThat(config.getValue()).isEqualTo("47065");
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            SystemConfiguration config = valido().build();

            config.disable();
            assertThat(config.isEnabled()).isFalse();
            config.disable();
            assertThat(config.isEnabled()).isFalse();

            config.enable();
            assertThat(config.isEnabled()).isTrue();
            config.enable();
            assertThat(config.isEnabled()).isTrue();
        }
    }
}
