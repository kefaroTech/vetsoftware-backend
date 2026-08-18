package com.vetsoftware.app.supplier.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CompanyRef — invariantes del value object")
class CompanyRefTest {

    @Test
    @DisplayName("conserva cada campo cuando los datos son validos")
    void conserva_cada_campo_cuando_los_datos_son_validos() {
        CompanyRef ref = new CompanyRef(10L, "Clinica Norte", "900123456");

        assertThat(ref.id()).isEqualTo(10L);
        assertThat(ref.name()).isEqualTo("Clinica Norte");
        assertThat(ref.identifier()).isEqualTo("900123456");
    }

    @Test
    @DisplayName("id nulo se rechaza")
    void id_nulo_se_rechaza() {
        assertThatThrownBy(() -> new CompanyRef(null, "Clinica Norte", "900123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company id is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("name nulo, vacio o en blanco se rechaza")
    void name_nulo_vacio_o_en_blanco_se_rechaza(String name) {
        assertThatThrownBy(() -> new CompanyRef(10L, name, "900123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company name is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("identifier nulo, vacio o en blanco se rechaza")
    void identifier_nulo_vacio_o_en_blanco_se_rechaza(String identifier) {
        assertThatThrownBy(() -> new CompanyRef(10L, "Clinica Norte", identifier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company identifier is required");
    }

    @ParameterizedTest
    @CsvSource({"1,Clinica A,NIT-1", "2,Clinica B,NIT-2"})
    @DisplayName("distintas combinaciones validas no lanzan excepcion")
    void distintas_combinaciones_validas_no_lanzan_excepcion(Long id, String name,
            String identifier) {
        assertThat(new CompanyRef(id, name, identifier)).isNotNull();
    }
}
