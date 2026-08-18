package com.vetsoftware.app.animalalert.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Companion VO propio de animalalert (no compartido con el {@code CompanyRef}
 * del modulo animal).
 */
@DisplayName("CompanyRef")
class CompanyRefTest {

    @Test
    @DisplayName("acepta id, nombre e identificador")
    void acepta_id_nombre_e_identificador() {
        CompanyRef ref = new CompanyRef(1L, "Clinica Norte", "NIT-900");

        assertThat(ref.id()).isEqualTo(1L);
        assertThat(ref.name()).isEqualTo("Clinica Norte");
        assertThat(ref.identifier()).isEqualTo("NIT-900");
    }

    @Test
    @DisplayName("rechaza id null")
    void rechaza_id_null() {
        assertThatThrownBy(() -> new CompanyRef(null, "Clinica", "NIT-900"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company id is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("rechaza nombre vacio")
    void rechaza_nombre_vacio(String nombre) {
        assertThatThrownBy(() -> new CompanyRef(1L, nombre, "NIT-900"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company name is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("rechaza identificador vacio")
    void rechaza_identificador_vacio(String identificador) {
        assertThatThrownBy(() -> new CompanyRef(1L, "Clinica", identificador))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company identifier is required");
    }
}
