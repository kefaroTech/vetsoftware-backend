package com.vetsoftware.app.country.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Country — invariantes y ciclo de vida")
class CountryTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 1, 15, 10, 30);

    private static Country colombia() {
        return new Country(1L, "Colombia", CREACION, true);
    }

    /** Nombres que el constructor y el update deben rechazar, con su motivo. */
    private static Stream<Arguments> nombresInvalidos() {
        return Stream.of(arguments("nulo", null, "name is required"),
                arguments("vacio", "", "name is required"),
                arguments("solo espacios", "   ", "name is required"),
                arguments("solo tabulador", "\t", "name is required"),
                arguments("101 caracteres", "C".repeat(101), "name must be 100 chars or less"));
    }

    @Nested
    @DisplayName("Construccion")
    class Construccion {

        @Test
        @DisplayName("conserva los cuatro campos tal y como se le pasan")
        void conserva_los_cuatro_campos() {
            Country country = colombia();

            assertThat(country.getId()).isEqualTo(1L);
            assertThat(country.getName()).isEqualTo("Colombia");
            assertThat(country.getCreatedDate()).isEqualTo(CREACION);
            assertThat(country.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("admite id nulo porque la entidad aun no se ha persistido")
        void admite_id_nulo() {
            Country country = new Country(null, "Colombia", CREACION, true);

            assertThat(country.getId()).isNull();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.country.domain.CountryTest#nombresInvalidos")
        @DisplayName("rechaza el nombre invalido")
        void rechaza_el_nombre_invalido(String caso, String nombre, String mensaje) {
            assertThatThrownBy(() -> new Country(1L, nombre, CREACION, true))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("acepta un nombre de exactamente 100 caracteres — el limite es inclusivo")
        void acepta_el_limite_de_100_caracteres() {
            String limite = "C".repeat(100);

            assertThat(new Country(1L, limite, CREACION, true).getName()).isEqualTo(limite);
        }

        @Test
        @DisplayName("no rechaza una fecha de creacion nula: no es una invariante del agregado")
        void admite_fecha_de_creacion_nula() {
            assertThatCode(() -> new Country(1L, "Colombia", null, true))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("puede nacer deshabilitado cuando se rehidrata desde persistencia")
        void puede_nacer_deshabilitado() {
            assertThat(new Country(1L, "Colombia", CREACION, false).isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Factoria create")
    class Creacion {

        @Test
        @DisplayName("crea el pais sin id, habilitado y fechado en el momento de la llamada")
        void crea_el_pais_sin_id_y_habilitado() {
            LocalDateTime antes = LocalDateTime.now();

            Country country = Country.create("Colombia");

            assertThat(country.getId()).isNull();
            assertThat(country.getName()).isEqualTo("Colombia");
            assertThat(country.isEnabled()).isTrue();
            assertThat(country.getCreatedDate()).isCloseTo(antes, within(10, ChronoUnit.SECONDS));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.country.domain.CountryTest#nombresInvalidos")
        @DisplayName("la factoria aplica las mismas invariantes que el constructor")
        void la_factoria_aplica_las_mismas_invariantes(String caso, String nombre, String mensaje) {
            assertThatThrownBy(() -> Country.create(nombre))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);
        }
    }

    @Nested
    @DisplayName("Actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("cambia el nombre y deja intactos id, fecha de creacion y estado")
        void cambia_solo_el_nombre() {
            Country country = colombia();

            country.update("Colombia Renombrada");

            assertThat(country.getName()).isEqualTo("Colombia Renombrada");
            assertThat(country.getId()).isEqualTo(1L);
            assertThat(country.getCreatedDate()).isEqualTo(CREACION);
            assertThat(country.isEnabled()).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.country.domain.CountryTest#nombresInvalidos")
        @DisplayName("rechaza el nombre invalido sin dejar el agregado a medias")
        void rechaza_el_nombre_invalido(String caso, String nombre, String mensaje) {
            Country country = colombia();

            assertThatThrownBy(() -> country.update(nombre))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);
            assertThat(country.getName()).isEqualTo("Colombia");
        }

        @Test
        @DisplayName("acepta un nombre de exactamente 100 caracteres")
        void acepta_el_limite_de_100_caracteres() {
            Country country = colombia();

            country.update("C".repeat(100));

            assertThat(country.getName()).hasSize(100);
        }
    }

    @Nested
    @DisplayName("Habilitar y deshabilitar")
    class Estado {

        @Test
        @DisplayName("disable deja el pais deshabilitado")
        void disable_deshabilita() {
            Country country = colombia();

            country.disable();

            assertThat(country.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("enable vuelve a habilitar un pais deshabilitado")
        void enable_habilita() {
            Country country = new Country(1L, "Colombia", CREACION, false);

            country.enable();

            assertThat(country.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("las dos operaciones son idempotentes")
        void son_idempotentes() {
            Country country = colombia();

            country.disable();
            country.disable();
            assertThat(country.isEnabled()).isFalse();

            country.enable();
            country.enable();
            assertThat(country.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Excepciones de dominio")
    class Excepciones {

        @Test
        @DisplayName("CountryNotFoundException lleva el id en el mensaje")
        void not_found_lleva_el_id() {
            assertThat(new CountryNotFoundException(42L)).hasMessageContaining("Country not found")
                    .hasMessageContaining("42");
        }

        @Test
        @DisplayName("CountryHasActiveChildrenException nombra el id y el tipo de hijo")
        void has_active_children_nombra_el_hijo() {
            assertThat(new CountryHasActiveChildrenException(42L, "state"))
                    .hasMessageContaining("42").hasMessageContaining("state");
        }
    }
}
