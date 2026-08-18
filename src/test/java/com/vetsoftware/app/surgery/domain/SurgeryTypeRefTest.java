package com.vetsoftware.app.surgery.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SurgeryTypeRef")
class SurgeryTypeRefTest {

    @Test
    @DisplayName("expone id y nombre")
    void expone_id_y_nombre() {
        SurgeryTypeRef ref = new SurgeryTypeRef(5L, "Ovariohisterectomia");

        assertThat(ref.id()).isEqualTo(5L);
        assertThat(ref.name()).isEqualTo("Ovariohisterectomia");
    }

    @Test
    @DisplayName("rechaza id nulo")
    void rechaza_id_nulo() {
        assertThatThrownBy(() -> new SurgeryTypeRef(null, "Ovariohisterectomia"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("surgery type id is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("rechaza el nombre en blanco")
    void rechaza_el_nombre_en_blanco(String nombre) {
        assertThatThrownBy(() -> new SurgeryTypeRef(5L, nombre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("surgery type name is required");
    }

    @Test
    @DisplayName("dos referencias con los mismos datos son iguales")
    void dos_referencias_con_los_mismos_datos_son_iguales() {
        assertThat(new SurgeryTypeRef(5L, "Ovariohisterectomia"))
                .isEqualTo(new SurgeryTypeRef(5L, "Ovariohisterectomia"));
    }
}
