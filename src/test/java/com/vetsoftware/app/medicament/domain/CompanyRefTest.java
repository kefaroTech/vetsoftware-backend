package com.vetsoftware.app.medicament.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("CompanyRef (medicament)")
class CompanyRefTest {

    @Test
    @DisplayName("construye con id, nombre e identificador validos")
    void construye_con_datos_validos() {
        CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "900123456");

        assertThat(ref.id()).isEqualTo(9L);
        assertThat(ref.name()).isEqualTo("Clinica Norte");
        assertThat(ref.identifier()).isEqualTo("900123456");
    }

    @Test
    @DisplayName("rechaza id nulo")
    void rechaza_id_nulo() {
        assertThatThrownBy(() -> new CompanyRef(null, "Clinica Norte", "900123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company id is required");
    }

    @ParameterizedTest
    @CsvSource({",Clinica Norte,900123456", "'   ',Clinica Norte,900123456"})
    @DisplayName("rechaza nombre nulo o en blanco")
    void rechaza_nombre_nulo_o_blanco(String name, String ignoredName, String identifier) {
        assertThatThrownBy(() -> new CompanyRef(9L, name, identifier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company name is required");
    }

    @ParameterizedTest
    @CsvSource({",Clinica Norte", "'   ',Clinica Norte"})
    @DisplayName("rechaza identificador nulo o en blanco")
    void rechaza_identificador_nulo_o_blanco(String identifier, String name) {
        assertThatThrownBy(() -> new CompanyRef(9L, name, identifier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company identifier is required");
    }
}
