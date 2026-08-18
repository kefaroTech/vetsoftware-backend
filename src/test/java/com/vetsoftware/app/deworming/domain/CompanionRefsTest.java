package com.vetsoftware.app.deworming.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Los companion VO son la frontera del vertical slicing: si dejan pasar un dato
 * incompleto de otra feature, el fallo aparece mucho mas tarde y lejos.
 */
@DisplayName("Companion VOs del modulo deworming")
class CompanionRefsTest {

    @Nested
    @DisplayName("AnimalRef")
    class AnimalRefTest {

        @Test
        @DisplayName("acepta id, nombre y codigo")
        void acepta_id_nombre_y_codigo() {
            AnimalRef ref = new AnimalRef(1L, "Firulais", "A-001");

            assertThat(ref.id()).isEqualTo(1L);
            assertThat(ref.name()).isEqualTo("Firulais");
            assertThat(ref.code()).isEqualTo("A-001");
        }

        @Test
        @DisplayName("rechaza id null")
        void rechaza_id_null() {
            assertThatThrownBy(() -> new AnimalRef(null, "Firulais", "A-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animal id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("rechaza nombre vacio")
        void rechaza_nombre_vacio(String nombre) {
            assertThatThrownBy(() -> new AnimalRef(1L, nombre, "A-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animal name is required");
        }

        @Test
        @DisplayName("el codigo es opcional — el animal puede no tenerlo")
        void el_codigo_es_opcional() {
            assertThatCode(() -> new AnimalRef(1L, "Firulais", null)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("CompanyRef")
    class CompanyRefTest {

        @Test
        @DisplayName("acepta id, nombre e identificador")
        void acepta_id_nombre_e_identificador() {
            CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "NIT-900");

            assertThat(ref.id()).isEqualTo(9L);
            assertThat(ref.name()).isEqualTo("Clinica Norte");
            assertThat(ref.identifier()).isEqualTo("NIT-900");
        }

        @Test
        @DisplayName("rechaza id null")
        void rechaza_id_null() {
            assertThatThrownBy(() -> new CompanyRef(null, "Clinica", "NIT-900"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("rechaza nombre vacio")
        void rechaza_nombre_vacio(String nombre) {
            assertThatThrownBy(() -> new CompanyRef(1L, nombre, "NIT-900"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company name is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("rechaza identificador vacio")
        void rechaza_identificador_vacio(String identificador) {
            assertThatThrownBy(() -> new CompanyRef(1L, "Clinica", identificador))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company identifier is required");
        }
    }

    @Nested
    @DisplayName("ConsultationRef")
    class ConsultationRefTest {

        @Test
        @DisplayName("acepta id y fecha")
        void acepta_id_y_fecha() {
            LocalDate fecha = LocalDate.of(2026, 3, 1);

            ConsultationRef ref = new ConsultationRef(200L, fecha);

            assertThat(ref.id()).isEqualTo(200L);
            assertThat(ref.date()).isEqualTo(fecha);
        }

        @Test
        @DisplayName("rechaza id null")
        void rechaza_id_null() {
            assertThatThrownBy(() -> new ConsultationRef(null, LocalDate.of(2026, 3, 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("consultation id is required");
        }

        @Test
        @DisplayName("rechaza fecha null")
        void rechaza_fecha_null() {
            assertThatThrownBy(() -> new ConsultationRef(200L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("consultation date is required");
        }
    }
}
