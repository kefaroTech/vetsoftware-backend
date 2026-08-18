package com.vetsoftware.app.surgery.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CompanyRef")
class CompanyRefTest {

    @Test
    @DisplayName("expone id, nombre e identificador")
    void expone_los_tres_campos() {
        CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "NIT-900");

        assertThat(ref.id()).isEqualTo(9L);
        assertThat(ref.name()).isEqualTo("Clinica Norte");
        assertThat(ref.identifier()).isEqualTo("NIT-900");
    }

    @Test
    @DisplayName("rechaza id nulo")
    void rechaza_id_nulo() {
        assertThatThrownBy(() -> new CompanyRef(null, "Clinica Norte", "NIT-900"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company id is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("rechaza el nombre en blanco")
    void rechaza_el_nombre_en_blanco(String nombre) {
        assertThatThrownBy(() -> new CompanyRef(9L, nombre, "NIT-900"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company name is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("rechaza el identificador en blanco: es el NIT que sale en el informe")
    void rechaza_el_identificador_en_blanco(String identificador) {
        assertThatThrownBy(() -> new CompanyRef(9L, "Clinica Norte", identificador))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company identifier is required");
    }

    @Test
    @DisplayName("dos referencias con los mismos datos son iguales")
    void dos_referencias_con_los_mismos_datos_son_iguales() {
        assertThat(new CompanyRef(9L, "Clinica Norte", "NIT-900"))
                .isEqualTo(new CompanyRef(9L, "Clinica Norte", "NIT-900"));
    }
}
