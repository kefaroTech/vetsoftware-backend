package com.vetsoftware.app.companysettings.domain;

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

@DisplayName("CompanySetting — invariantes del ajuste por empresa")
class CompanySettingTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 1L;
        private Long companyId = 9L;
        private String propertyName = "inventory.allow_negative_stock";
        private String value = "true";
        private final LocalDateTime createdDate = CREADO;
        private final boolean enabled = true;

        private Builder companyId(Long v) {
            this.companyId = v;
            return this;
        }

        private Builder propertyName(String v) {
            this.propertyName = v;
            return this;
        }

        private Builder value(String v) {
            this.value = v;
            return this;
        }

        private CompanySetting build() {
            return new CompanySetting(id, companyId, propertyName, value, createdDate, enabled);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            CompanySetting setting = valido().build();

            assertThat(setting.getId()).isEqualTo(1L);
            assertThat(setting.getCompanyId()).isEqualTo(9L);
            assertThat(setting.getPropertyName()).isEqualTo("inventory.allow_negative_stock");
            assertThat(setting.getValue()).isEqualTo("true");
            assertThat(setting.getCreatedDate()).isEqualTo(CREADO);
            assertThat(setting.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y con fecha actual")
        void create_nace_sin_id_habilitado_y_con_fecha_actual() {
            CompanySetting setting = CompanySetting.create(9L, "inventory.allow_negative_stock",
                    "true");

            assertThat(setting.getId()).isNull();
            assertThat(setting.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada
            // en "Determinismo" del CLAUDE.md.
            assertThat(setting.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("assignId reemplaza el id tras persistir")
        void assign_id_reemplaza_el_id_tras_persistir() {
            CompanySetting setting = CompanySetting.create(9L, "k", "v");

            setting.assignId(42L);

            assertThat(setting.getId()).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas por el constructor")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("companyId null",
                            (ThrowingCallable) () -> valido().companyId(null).build(),
                            "companyId is required"),
                    arguments("propertyName null",
                            (ThrowingCallable) () -> valido().propertyName(null).build(),
                            "propertyName is required"),
                    arguments("propertyName vacio",
                            (ThrowingCallable) () -> valido().propertyName("").build(),
                            "propertyName is required"),
                    arguments("propertyName en blanco",
                            (ThrowingCallable) () -> valido().propertyName("   ").build(),
                            "propertyName is required"),
                    arguments("propertyName de 101 chars",
                            (ThrowingCallable) () -> valido().propertyName("x".repeat(101)).build(),
                            "propertyName must be 100 chars or less"),
                    arguments("value null", (ThrowingCallable) () -> valido().value(null).build(),
                            "value is required"),
                    arguments("value de 256 chars",
                            (ThrowingCallable) () -> valido().value("x".repeat(256)).build(),
                            "value must be 255 chars or less"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("propertyName de 100 chars exactos se acepta")
        void property_name_de_100_chars_se_acepta() {
            assertThatCode(() -> valido().propertyName("x".repeat(100)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("value de 255 chars exactos se acepta")
        void value_de_255_chars_se_acepta() {
            assertThatCode(() -> valido().value("x".repeat(255)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("value vacio (no en blanco) es valido: solo propertyName exige contenido")
        void value_vacio_es_valido() {
            // El constructor solo chequea null y longitud para value, no isBlank() como
            // hace con propertyName. Fijado a proposito: un ajuste con valor "" es
            // distinto de ausencia de fila, y el dominio no lo prohibe.
            assertThatCode(() -> valido().value("").build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("updateValue")
    class UpdateValue {

        @Test
        @DisplayName("reemplaza el valor y conserva el resto del agregado")
        void reemplaza_el_valor_y_conserva_el_resto() {
            CompanySetting setting = valido().build();

            setting.updateValue("false");

            assertThat(setting.getValue()).isEqualTo("false");
            assertThat(setting.getId()).isEqualTo(1L);
            assertThat(setting.getCompanyId()).isEqualTo(9L);
            assertThat(setting.getPropertyName()).isEqualTo("inventory.allow_negative_stock");
            assertThat(setting.getCreatedDate()).isEqualTo(CREADO);
        }

        @Test
        @DisplayName("rechaza un valor null y no deja el ajuste a medias")
        void rechaza_un_valor_null() {
            CompanySetting setting = valido().build();

            assertThatThrownBy(() -> setting.updateValue(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("value is required");

            assertThat(setting.getValue()).isEqualTo("true");
        }

        @Test
        @DisplayName("rechaza un valor de mas de 255 caracteres")
        void rechaza_un_valor_demasiado_largo() {
            CompanySetting setting = valido().build();

            assertThatThrownBy(() -> setting.updateValue("x".repeat(256)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("value must be 255 chars or less");

            assertThat(setting.getValue()).isEqualTo("true");
        }

        @Test
        @DisplayName("un valor de 255 chars exactos se acepta")
        void un_valor_de_255_chars_se_acepta() {
            CompanySetting setting = valido().build();

            assertThatCode(() -> setting.updateValue("x".repeat(255))).doesNotThrowAnyException();
        }
    }
}
