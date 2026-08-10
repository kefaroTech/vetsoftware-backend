package com.vetsoftware.app.hospitalization.domain;

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
 * Companion VOs de la feature: son la unica defensa contra que una FK a otra
 * feature entre a medio rellenar, porque el repositorio usa
 * {@code getReferenceById} sin validar.
 */
@DisplayName("Companion VOs de hospitalization")
class HospitalizationRefsTest {

    @Nested
    @DisplayName("AnimalRef")
    class Animal {

        @Test
        @DisplayName("conserva id, nombre y codigo")
        void conserva_id_nombre_y_codigo() {
            AnimalRef ref = new AnimalRef(3L, "Firulais", "A-001");

            assertThat(ref.id()).isEqualTo(3L);
            assertThat(ref.name()).isEqualTo("Firulais");
            assertThat(ref.code()).isEqualTo("A-001");
        }

        @Test
        @DisplayName("exige id")
        void exige_id() {
            assertThatThrownBy(() -> new AnimalRef(null, "Firulais", "A-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animal id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("exige nombre no vacio")
        void exige_nombre_no_vacio(String nombre) {
            assertThatThrownBy(() -> new AnimalRef(3L, nombre, "A-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animal name is required");
        }

        @Test
        @DisplayName("el codigo es opcional: un animal sin codigo sigue siendo referenciable")
        void el_codigo_es_opcional() {
            assertThatCode(() -> new AnimalRef(3L, "Firulais", null)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("CompanyRef")
    class Company {

        @Test
        @DisplayName("conserva id, nombre e identificador")
        void conserva_id_nombre_e_identificador() {
            CompanyRef ref = new CompanyRef(9L, "Clinica Vet", "900123456");

            assertThat(ref.id()).isEqualTo(9L);
            assertThat(ref.name()).isEqualTo("Clinica Vet");
            assertThat(ref.identifier()).isEqualTo("900123456");
        }

        @Test
        @DisplayName("exige id")
        void exige_id() {
            assertThatThrownBy(() -> new CompanyRef(null, "Clinica Vet", "900123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("exige nombre no vacio")
        void exige_nombre_no_vacio(String nombre) {
            assertThatThrownBy(() -> new CompanyRef(9L, nombre, "900123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company name is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("exige identificador no vacio")
        void exige_identificador_no_vacio(String identificador) {
            assertThatThrownBy(() -> new CompanyRef(9L, "Clinica Vet", identificador))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company identifier is required");
        }
    }

    @Nested
    @DisplayName("ConsultationRef")
    class Consultation {

        @Test
        @DisplayName("conserva id y fecha")
        void conserva_id_y_fecha() {
            ConsultationRef ref = new ConsultationRef(7L, LocalDate.of(2026, 2, 28));

            assertThat(ref.id()).isEqualTo(7L);
            assertThat(ref.date()).isEqualTo(LocalDate.of(2026, 2, 28));
        }

        @Test
        @DisplayName("exige id")
        void exige_id() {
            assertThatThrownBy(() -> new ConsultationRef(null, LocalDate.of(2026, 2, 28)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("consultation id is required");
        }

        @Test
        @DisplayName("exige fecha")
        void exige_fecha() {
            assertThatThrownBy(() -> new ConsultationRef(7L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("consultation date is required");
        }
    }
}
