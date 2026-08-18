package com.vetsoftware.app.permission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CompanyRef — invariantes")
class CompanyRefTest {

    @Test
    @DisplayName("conserva los campos validos")
    void conserva_los_campos_validos() {
        CompanyRef ref = new CompanyRef(1L, "Clinica Norte", "NIT-900");

        assertThat(ref.id()).isEqualTo(1L);
        assertThat(ref.name()).isEqualTo("Clinica Norte");
        assertThat(ref.identifier()).isEqualTo("NIT-900");
    }

    @Test
    @DisplayName("id null se rechaza")
    void id_null_se_rechaza() {
        assertThatThrownBy(() -> new CompanyRef(null, "Clinica Norte", "NIT-900"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company id is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("name en blanco se rechaza")
    void name_en_blanco_se_rechaza(String name) {
        assertThatThrownBy(() -> new CompanyRef(1L, name, "NIT-900"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company name is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("identifier en blanco se rechaza")
    void identifier_en_blanco_se_rechaza(String identifier) {
        assertThatThrownBy(() -> new CompanyRef(1L, "Clinica Norte", identifier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company identifier is required");
    }
}
