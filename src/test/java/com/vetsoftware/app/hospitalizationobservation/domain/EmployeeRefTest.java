package com.vetsoftware.app.hospitalizationobservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("EmployeeRef — invariantes del value object")
class EmployeeRefTest {

    @Test
    @DisplayName("el constructor compacto conserva cada campo")
    void el_constructor_compacto_conserva_cada_campo() {
        EmployeeRef ref = new EmployeeRef(4L, "EMP-001", "Ana Ruiz");

        assertThat(ref.id()).isEqualTo(4L);
        assertThat(ref.employeeCode()).isEqualTo("EMP-001");
        assertThat(ref.name()).isEqualTo("Ana Ruiz");
    }

    @Test
    @DisplayName("id nulo se rechaza")
    void id_nulo_se_rechaza() {
        assertThatThrownBy(() -> new EmployeeRef(null, "EMP-001", "Ana Ruiz"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("employee id is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("employeeCode nulo o en blanco se rechaza")
    void employeeCode_nulo_o_en_blanco_se_rechaza(String valor) {
        assertThatThrownBy(() -> new EmployeeRef(4L, valor, "Ana Ruiz"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("employee code is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("name nulo o en blanco se rechaza")
    void name_nulo_o_en_blanco_se_rechaza(String valor) {
        assertThatThrownBy(() -> new EmployeeRef(4L, "EMP-001", valor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("employee name is required");
    }

    @Test
    @DisplayName("dos refs con los mismos valores son iguales")
    void dos_refs_con_los_mismos_valores_son_iguales() {
        assertThat(new EmployeeRef(4L, "EMP-001", "Ana Ruiz"))
                .isEqualTo(new EmployeeRef(4L, "EMP-001", "Ana Ruiz"));
    }
}
