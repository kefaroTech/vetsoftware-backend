package com.vetsoftware.app.consultation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AnimalRef — companion VO de la feature animal")
class AnimalRefTest {

    @Test
    @DisplayName("constructor publico conserva cada campo")
    void constructor_publico_conserva_cada_campo() {
        AnimalRef ref = new AnimalRef(1L, "Firulais", "A-001");

        assertThat(ref.id()).isEqualTo(1L);
        assertThat(ref.name()).isEqualTo("Firulais");
        assertThat(ref.code()).isEqualTo("A-001");
    }

    @Test
    @DisplayName("code es opcional")
    void code_es_opcional() {
        AnimalRef ref = new AnimalRef(1L, "Firulais", null);

        assertThat(ref.code()).isNull();
    }

    @Test
    @DisplayName("id null lanza excepcion")
    void id_null_lanza_excepcion() {
        assertThatThrownBy(() -> new AnimalRef(null, "Firulais", "A-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("animal id is required");
    }

    @Test
    @DisplayName("name null lanza excepcion")
    void name_null_lanza_excepcion() {
        assertThatThrownBy(() -> new AnimalRef(1L, null, "A-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("animal name is required");
    }

    @Test
    @DisplayName("name en blanco lanza excepcion")
    void name_en_blanco_lanza_excepcion() {
        assertThatThrownBy(() -> new AnimalRef(1L, "   ", "A-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("animal name is required");
    }
}
