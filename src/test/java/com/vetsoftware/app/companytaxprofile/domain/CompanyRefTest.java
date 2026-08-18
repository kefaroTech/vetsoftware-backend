package com.vetsoftware.app.companytaxprofile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Companion VO de la empresa dueña del perfil fiscal. Es la frontera del
 * vertical slicing con el modulo {@code company}: si deja pasar un dato
 * incompleto, el fallo aparece mucho mas tarde y mucho mas lejos de aqui.
 */
@DisplayName("CompanyRef")
class CompanyRefTest {

    @Test
    @DisplayName("acepta id, nombre e identificador y los expone tal cual")
    void acepta_id_nombre_e_identificador() {
        CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "900123456-8");

        assertThat(ref.id()).isEqualTo(9L);
        assertThat(ref.name()).isEqualTo("Clinica Norte");
        assertThat(ref.identifier()).isEqualTo("900123456-8");
    }

    @Test
    @DisplayName("rechaza id null")
    void rechaza_id_null() {
        assertThatThrownBy(() -> new CompanyRef(null, "Clinica Norte", "900123456-8"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company id is required");
    }

    @ParameterizedTest(name = "name=[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("rechaza nombre vacio o en blanco")
    void rechaza_nombre_vacio(String nombre) {
        assertThatThrownBy(() -> new CompanyRef(9L, nombre, "900123456-8"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company name is required");
    }

    @ParameterizedTest(name = "identifier=[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("rechaza identificador vacio o en blanco")
    void rechaza_identificador_vacio(String identificador) {
        assertThatThrownBy(() -> new CompanyRef(9L, "Clinica Norte", identificador))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company identifier is required");
    }
}
