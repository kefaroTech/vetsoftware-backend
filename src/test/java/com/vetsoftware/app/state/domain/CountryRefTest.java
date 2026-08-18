package com.vetsoftware.app.state.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("CountryRef — companion VO de pais")
class CountryRefTest {

    private static Stream<Arguments> nombresInvalidos() {
        return Stream.of(arguments("nulo", (String) null), arguments("vacio", ""),
                arguments("solo espacios", "   "));
    }

    @Test
    @DisplayName("conserva id y nombre tal y como se le pasan")
    void conserva_id_y_nombre() {
        CountryRef ref = new CountryRef(7L, "Colombia");

        assertThat(ref.id()).isEqualTo(7L);
        assertThat(ref.name()).isEqualTo("Colombia");
    }

    @Test
    @DisplayName("rechaza un id nulo")
    void rechaza_id_nulo() {
        assertThatThrownBy(() -> new CountryRef(null, "Colombia"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("country id is required");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.vetsoftware.app.state.domain.CountryRefTest#nombresInvalidos")
    @DisplayName("rechaza un nombre invalido")
    void rechaza_nombre_invalido(String caso, String nombre) {
        assertThatThrownBy(() -> new CountryRef(7L, nombre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("country name is required");
    }

    @Test
    @DisplayName("dos referencias con los mismos valores son iguales — semantica de record")
    void dos_referencias_iguales_son_iguales() {
        assertThat(new CountryRef(7L, "Colombia")).isEqualTo(new CountryRef(7L, "Colombia"));
    }
}
