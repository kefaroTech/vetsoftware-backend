package com.vetsoftware.app.animalalert.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Companion VO propio de animalalert (no compartido con el {@code AnimalRef}
 * del modulo animal): si dejara pasar un animal incompleto, el fallo aparece
 * mucho mas tarde y lejos de esta feature.
 */
@DisplayName("AnimalRef")
class AnimalRefTest {

    @Test
    @DisplayName("acepta id, nombre y codigo")
    void acepta_id_nombre_y_codigo() {
        AnimalRef ref = new AnimalRef(1L, "Firulais", "A-001");

        assertThat(ref.id()).isEqualTo(1L);
        assertThat(ref.name()).isEqualTo("Firulais");
        assertThat(ref.code()).isEqualTo("A-001");
    }

    @Test
    @DisplayName("el codigo es opcional")
    void el_codigo_es_opcional() {
        assertThatCode(() -> new AnimalRef(1L, "Firulais", null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rechaza id null")
    void rechaza_id_null() {
        assertThatThrownBy(() -> new AnimalRef(null, "Firulais", "A-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("animal id is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("rechaza nombre vacio")
    void rechaza_nombre_vacio(String nombre) {
        assertThatThrownBy(() -> new AnimalRef(1L, nombre, "A-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("animal name is required");
    }
}
