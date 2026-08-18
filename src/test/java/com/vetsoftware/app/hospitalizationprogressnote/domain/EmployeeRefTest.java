package com.vetsoftware.app.hospitalizationprogressnote.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Companion VO hacia employee. Es la unica defensa contra que la FK entre a
 * medio rellenar, porque el repositorio la carga con {@code getReferenceById}
 * sin validar.
 */
@DisplayName("EmployeeRef")
class EmployeeRefTest {

    @Test
    @DisplayName("conserva id, codigo y nombre")
    void conserva_id_codigo_y_nombre() {
        EmployeeRef ref = new EmployeeRef(4L, "EMP-001", "Ana Ruiz");

        assertThat(ref.id()).isEqualTo(4L);
        assertThat(ref.employeeCode()).isEqualTo("EMP-001");
        assertThat(ref.name()).isEqualTo("Ana Ruiz");
    }

    @Test
    @DisplayName("rechaza id null")
    void rechaza_id_null() {
        assertThatThrownBy(() -> new EmployeeRef(null, "EMP-001", "Ana Ruiz"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("employee id is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("rechaza codigo de empleado vacio")
    void rechaza_codigo_vacio(String codigo) {
        assertThatThrownBy(() -> new EmployeeRef(4L, codigo, "Ana Ruiz"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("employee code is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("rechaza nombre vacio")
    void rechaza_nombre_vacio(String nombre) {
        assertThatThrownBy(() -> new EmployeeRef(4L, "EMP-001", nombre))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("employee name is required");
    }
}
