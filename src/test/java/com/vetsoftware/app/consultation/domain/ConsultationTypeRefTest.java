package com.vetsoftware.app.consultation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsultationTypeRef — companion VO de la feature consultationtype")
class ConsultationTypeRefTest {

    @Test
    @DisplayName("constructor publico conserva cada campo")
    void constructor_publico_conserva_cada_campo() {
        ConsultationTypeRef ref = new ConsultationTypeRef(5L, "Control");

        assertThat(ref.id()).isEqualTo(5L);
        assertThat(ref.name()).isEqualTo("Control");
    }

    @Test
    @DisplayName("id null lanza excepcion")
    void id_null_lanza_excepcion() {
        assertThatThrownBy(() -> new ConsultationTypeRef(null, "Control"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consultation type id is required");
    }

    @Test
    @DisplayName("name null lanza excepcion")
    void name_null_lanza_excepcion() {
        assertThatThrownBy(() -> new ConsultationTypeRef(5L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consultation type name is required");
    }

    @Test
    @DisplayName("name en blanco lanza excepcion")
    void name_en_blanco_lanza_excepcion() {
        assertThatThrownBy(() -> new ConsultationTypeRef(5L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consultation type name is required");
    }
}
