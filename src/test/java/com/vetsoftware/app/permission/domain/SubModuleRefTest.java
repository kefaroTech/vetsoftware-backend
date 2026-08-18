package com.vetsoftware.app.permission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SubModuleRef — invariantes")
class SubModuleRefTest {

    @Test
    @DisplayName("conserva los campos validos")
    void conserva_los_campos_validos() {
        SubModuleRef ref = new SubModuleRef(1L, "Inventario", "INV");

        assertThat(ref.id()).isEqualTo(1L);
        assertThat(ref.name()).isEqualTo("Inventario");
        assertThat(ref.code()).isEqualTo("INV");
    }

    @Test
    @DisplayName("id null se rechaza")
    void id_null_se_rechaza() {
        assertThatThrownBy(() -> new SubModuleRef(null, "Inventario", "INV"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subModule id is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("name en blanco se rechaza")
    void name_en_blanco_se_rechaza(String name) {
        assertThatThrownBy(() -> new SubModuleRef(1L, name, "INV"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subModule name is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("code en blanco se rechaza")
    void code_en_blanco_se_rechaza(String code) {
        assertThatThrownBy(() -> new SubModuleRef(1L, "Inventario", code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subModule code is required");
    }
}
