package com.vetsoftware.app.consultation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyRef — companion VO de la feature company")
class CompanyRefTest {

    @Test
    @DisplayName("constructor publico conserva cada campo")
    void constructor_publico_conserva_cada_campo() {
        CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "NIT-900");

        assertThat(ref.id()).isEqualTo(9L);
        assertThat(ref.name()).isEqualTo("Clinica Norte");
        assertThat(ref.identifier()).isEqualTo("NIT-900");
    }

    @Test
    @DisplayName("id null lanza excepcion")
    void id_null_lanza_excepcion() {
        assertThatThrownBy(() -> new CompanyRef(null, "Clinica Norte", "NIT-900"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company id is required");
    }

    @Test
    @DisplayName("name null lanza excepcion")
    void name_null_lanza_excepcion() {
        assertThatThrownBy(() -> new CompanyRef(9L, null, "NIT-900"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company name is required");
    }

    @Test
    @DisplayName("name en blanco lanza excepcion")
    void name_en_blanco_lanza_excepcion() {
        assertThatThrownBy(() -> new CompanyRef(9L, "   ", "NIT-900"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company name is required");
    }

    @Test
    @DisplayName("identifier null lanza excepcion")
    void identifier_null_lanza_excepcion() {
        assertThatThrownBy(() -> new CompanyRef(9L, "Clinica Norte", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company identifier is required");
    }

    @Test
    @DisplayName("identifier en blanco lanza excepcion")
    void identifier_en_blanco_lanza_excepcion() {
        assertThatThrownBy(() -> new CompanyRef(9L, "Clinica Norte", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company identifier is required");
    }
}
