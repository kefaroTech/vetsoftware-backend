package com.vetsoftware.app.owner.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyRef — companion VO de company")
class CompanyRefTest {

    @Test
    @DisplayName("conserva cada campo tal y como se le entrega")
    void conserva_cada_campo_tal_y_como_se_le_entrega() {
        CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "NIT-900123456");

        assertThat(ref.id()).isEqualTo(9L);
        assertThat(ref.name()).isEqualTo("Clinica Norte");
        assertThat(ref.identifier()).isEqualTo("NIT-900123456");
    }

    @Test
    @DisplayName("rechaza id nulo")
    void rechaza_id_nulo() {
        assertThatThrownBy(() -> new CompanyRef(null, "Clinica Norte", "NIT-900123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company id is required");
    }

    @Test
    @DisplayName("rechaza nombre nulo")
    void rechaza_nombre_nulo() {
        assertThatThrownBy(() -> new CompanyRef(9L, null, "NIT-900123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company name is required");
    }

    @Test
    @DisplayName("rechaza nombre en blanco")
    void rechaza_nombre_en_blanco() {
        assertThatThrownBy(() -> new CompanyRef(9L, "   ", "NIT-900123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company name is required");
    }

    @Test
    @DisplayName("rechaza identificador nulo")
    void rechaza_identificador_nulo() {
        assertThatThrownBy(() -> new CompanyRef(9L, "Clinica Norte", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company identifier is required");
    }

    @Test
    @DisplayName("rechaza identificador en blanco")
    void rechaza_identificador_en_blanco() {
        assertThatThrownBy(() -> new CompanyRef(9L, "Clinica Norte", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company identifier is required");
    }
}
