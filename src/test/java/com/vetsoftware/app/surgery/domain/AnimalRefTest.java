package com.vetsoftware.app.surgery.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AnimalRef")
class AnimalRefTest {

    @Test
    @DisplayName("expone id, nombre y codigo tal cual")
    void expone_los_tres_campos() {
        AnimalRef ref = new AnimalRef(100L, "Firulais", "A-001");

        assertThat(ref.id()).isEqualTo(100L);
        assertThat(ref.name()).isEqualTo("Firulais");
        assertThat(ref.code()).isEqualTo("A-001");
    }

    @Test
    @DisplayName("rechaza id nulo")
    void rechaza_id_nulo() {
        assertThatThrownBy(() -> new AnimalRef(null, "Firulais", "A-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("animal id is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("rechaza el nombre en blanco")
    void rechaza_el_nombre_en_blanco(String nombre) {
        assertThatThrownBy(() -> new AnimalRef(100L, nombre, "A-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("animal name is required");
    }

    @Test
    @DisplayName("el codigo es opcional: hay animales sin codigo asignado")
    void el_codigo_es_opcional() {
        assertThatCode(() -> new AnimalRef(100L, "Firulais", null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("dos referencias con los mismos datos son iguales")
    void dos_referencias_con_los_mismos_datos_son_iguales() {
        assertThat(new AnimalRef(100L, "Firulais", "A-001"))
                .isEqualTo(new AnimalRef(100L, "Firulais", "A-001"));
    }
}
