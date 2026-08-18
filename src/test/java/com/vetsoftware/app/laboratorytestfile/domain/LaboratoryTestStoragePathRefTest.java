package com.vetsoftware.app.laboratorytestfile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("LaboratoryTestStoragePathRef — invariantes del value object")
class LaboratoryTestStoragePathRefTest {

    @Test
    @DisplayName("el constructor compacto conserva cada campo")
    void el_constructor_compacto_conserva_cada_campo() {
        LaboratoryTestStoragePathRef ref = new LaboratoryTestStoragePathRef(9L, 3L, 100L,
                "Firulais");

        assertThat(ref.companyId()).isEqualTo(9L);
        assertThat(ref.ownerId()).isEqualTo(3L);
        assertThat(ref.animalId()).isEqualTo(100L);
        assertThat(ref.animalName()).isEqualTo("Firulais");
    }

    @Test
    @DisplayName("companyId nulo se rechaza")
    void companyId_nulo_se_rechaza() {
        assertThatThrownBy(() -> new LaboratoryTestStoragePathRef(null, 3L, 100L, "Firulais"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("companyId is required");
    }

    @Test
    @DisplayName("ownerId nulo se rechaza")
    void ownerId_nulo_se_rechaza() {
        assertThatThrownBy(() -> new LaboratoryTestStoragePathRef(9L, null, 100L, "Firulais"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ownerId is required");
    }

    @Test
    @DisplayName("animalId nulo se rechaza")
    void animalId_nulo_se_rechaza() {
        assertThatThrownBy(() -> new LaboratoryTestStoragePathRef(9L, 3L, null, "Firulais"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("animalId is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("animalName nulo o en blanco se rechaza")
    void animalName_nulo_o_en_blanco_se_rechaza(String valor) {
        assertThatThrownBy(() -> new LaboratoryTestStoragePathRef(9L, 3L, 100L, valor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("animalName is required");
    }

    @Test
    @DisplayName("dos refs con los mismos valores son iguales")
    void dos_refs_con_los_mismos_valores_son_iguales() {
        LaboratoryTestStoragePathRef a = new LaboratoryTestStoragePathRef(9L, 3L, 100L, "Firulais");
        LaboratoryTestStoragePathRef b = new LaboratoryTestStoragePathRef(9L, 3L, 100L, "Firulais");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
