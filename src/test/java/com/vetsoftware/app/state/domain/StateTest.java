package com.vetsoftware.app.state.domain;

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

@DisplayName("State — invariantes y ciclo de vida")
class StateTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final CountryRef COLOMBIA = new CountryRef(1L, "Colombia");
    private static final CountryRef CHILE = new CountryRef(2L, "Chile");

    private static State antioquia() {
        return new State(1L, "Antioquia", COLOMBIA, "05", CREACION, true);
    }

    /** Nombres que el constructor, el update y la factoria deben rechazar. */
    private static Stream<Arguments> nombresInvalidos() {
        return Stream.of(arguments("nulo", null, "name is required"),
                arguments("vacio", "", "name is required"),
                arguments("solo espacios", "   ", "name is required"),
                arguments("solo tabulador", "\t", "name is required"),
                arguments("101 caracteres", "A".repeat(101), "name must be 100 chars or less"));
    }

    @Nested
    @DisplayName("Construccion")
    class Construccion {

        @Test
        @DisplayName("conserva los seis campos tal y como se le pasan")
        void conserva_los_seis_campos() {
            State state = antioquia();

            assertThat(state.getId()).isEqualTo(1L);
            assertThat(state.getName()).isEqualTo("Antioquia");
            assertThat(state.getCountry()).isEqualTo(COLOMBIA);
            assertThat(state.getDaneCode()).isEqualTo("05");
            assertThat(state.getCreatedDate()).isEqualTo(CREACION);
            assertThat(state.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("admite id nulo porque la entidad aun no se ha persistido")
        void admite_id_nulo() {
            State state = new State(null, "Antioquia", COLOMBIA, "05", CREACION, true);

            assertThat(state.getId()).isNull();
        }

        @Test
        @DisplayName("admite un codigo dane nulo: no es una invariante del agregado")
        void admite_dane_code_nulo() {
            assertThatCode(() -> new State(1L, "Antioquia", COLOMBIA, null, CREACION, true))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.state.domain.StateTest#nombresInvalidos")
        @DisplayName("rechaza el nombre invalido")
        void rechaza_el_nombre_invalido(String caso, String nombre, String mensaje) {
            assertThatThrownBy(() -> new State(1L, nombre, COLOMBIA, "05", CREACION, true))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("acepta un nombre de exactamente 100 caracteres — el limite es inclusivo")
        void acepta_el_limite_de_100_caracteres() {
            String limite = "A".repeat(100);

            assertThat(new State(1L, limite, COLOMBIA, "05", CREACION, true).getName())
                    .isEqualTo(limite);
        }

        @Test
        @DisplayName("rechaza un pais nulo")
        void rechaza_el_pais_nulo() {
            assertThatThrownBy(() -> new State(1L, "Antioquia", null, "05", CREACION, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("country is required");
        }

        @Test
        @DisplayName("no rechaza una fecha de creacion nula: no es una invariante del agregado")
        void admite_fecha_de_creacion_nula() {
            assertThatCode(() -> new State(1L, "Antioquia", COLOMBIA, "05", null, true))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("puede nacer deshabilitado cuando se rehidrata desde persistencia")
        void puede_nacer_deshabilitado() {
            assertThat(new State(1L, "Antioquia", COLOMBIA, "05", CREACION, false).isEnabled())
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("Factoria create")
    class Creacion {

        @Test
        @DisplayName("crea el departamento sin id, habilitado y fechado en el momento de la llamada")
        void crea_el_departamento_sin_id_y_habilitado() {
            LocalDateTime antes = LocalDateTime.now();

            State state = State.create("Antioquia", COLOMBIA, "05");

            assertThat(state.getId()).isNull();
            assertThat(state.getName()).isEqualTo("Antioquia");
            assertThat(state.getCountry()).isEqualTo(COLOMBIA);
            assertThat(state.getDaneCode()).isEqualTo("05");
            assertThat(state.isEnabled()).isTrue();
            assertThat(state.getCreatedDate()).isCloseTo(antes, within(10, ChronoUnit.SECONDS));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.state.domain.StateTest#nombresInvalidos")
        @DisplayName("la factoria aplica las mismas invariantes que el constructor")
        void la_factoria_aplica_las_mismas_invariantes(String caso, String nombre, String mensaje) {
            assertThatThrownBy(() -> State.create(nombre, COLOMBIA, "05"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("la factoria tambien rechaza un pais nulo")
        void la_factoria_rechaza_el_pais_nulo() {
            assertThatThrownBy(() -> State.create("Antioquia", null, "05"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("country is required");
        }
    }

    @Nested
    @DisplayName("Actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("cambia nombre, pais y codigo dane; deja intactos id, fecha de creacion y estado")
        void cambia_los_campos_editables() {
            State state = antioquia();

            state.update("Antioquia Renombrada", CHILE, "06");

            assertThat(state.getName()).isEqualTo("Antioquia Renombrada");
            assertThat(state.getCountry()).isEqualTo(CHILE);
            assertThat(state.getDaneCode()).isEqualTo("06");
            assertThat(state.getId()).isEqualTo(1L);
            assertThat(state.getCreatedDate()).isEqualTo(CREACION);
            assertThat(state.isEnabled()).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.state.domain.StateTest#nombresInvalidos")
        @DisplayName("rechaza el nombre invalido sin dejar el agregado a medias")
        void rechaza_el_nombre_invalido(String caso, String nombre, String mensaje) {
            State state = antioquia();

            assertThatThrownBy(() -> state.update(nombre, COLOMBIA, "05"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);
            assertThat(state.getName()).isEqualTo("Antioquia");
        }

        @Test
        @DisplayName("rechaza un pais nulo sin dejar el agregado a medias")
        void rechaza_el_pais_nulo() {
            State state = antioquia();

            assertThatThrownBy(() -> state.update("Antioquia", null, "05"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("country is required");
            assertThat(state.getCountry()).isEqualTo(COLOMBIA);
        }

        @Test
        @DisplayName("acepta un nombre de exactamente 100 caracteres")
        void acepta_el_limite_de_100_caracteres() {
            State state = antioquia();

            state.update("A".repeat(100), COLOMBIA, "05");

            assertThat(state.getName()).hasSize(100);
        }
    }

    @Nested
    @DisplayName("Habilitar y deshabilitar")
    class Estado {

        @Test
        @DisplayName("disable deja el departamento deshabilitado")
        void disable_deshabilita() {
            State state = antioquia();

            state.disable();

            assertThat(state.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("enable vuelve a habilitar un departamento deshabilitado")
        void enable_habilita() {
            State state = new State(1L, "Antioquia", COLOMBIA, "05", CREACION, false);

            state.enable();

            assertThat(state.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("las dos operaciones son idempotentes")
        void son_idempotentes() {
            State state = antioquia();

            state.disable();
            state.disable();
            assertThat(state.isEnabled()).isFalse();

            state.enable();
            state.enable();
            assertThat(state.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Excepciones de dominio")
    class Excepciones {

        @Test
        @DisplayName("StateNotFoundException lleva el id en el mensaje")
        void not_found_lleva_el_id() {
            assertThat(new StateNotFoundException(42L)).hasMessageContaining("State not found")
                    .hasMessageContaining("42");
        }

        @Test
        @DisplayName("StateHasActiveChildrenException nombra el id y el tipo de hijo")
        void has_active_children_nombra_el_hijo() {
            assertThat(new StateHasActiveChildrenException(42L, "city")).hasMessageContaining("42")
                    .hasMessageContaining("city");
        }
    }
}
