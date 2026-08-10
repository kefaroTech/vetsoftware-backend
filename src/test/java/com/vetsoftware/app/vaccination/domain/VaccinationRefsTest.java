package com.vetsoftware.app.vaccination.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Los companion VO son la unica frontera con las otras features: si dejan pasar
 * un id nulo, el fallo aparece mucho mas tarde y lejos de aqui.
 */
@DisplayName("Value objects de vaccination")
class VaccinationRefsTest {

    @Nested
    @DisplayName("AnimalRef")
    class Animal {

        @Test
        @DisplayName("conserva id, nombre y codigo")
        void conserva_sus_campos() {
            AnimalRef ref = new AnimalRef(2L, "Firulais", "A-001");

            assertThat(ref.id()).isEqualTo(2L);
            assertThat(ref.name()).isEqualTo("Firulais");
            assertThat(ref.code()).isEqualTo("A-001");
        }

        @Test
        @DisplayName("admite codigo nulo porque el animal puede no tenerlo")
        void admite_codigo_nulo() {
            assertThat(new AnimalRef(2L, "Firulais", null).code()).isNull();
        }

        @Test
        @DisplayName("rechaza id nulo")
        void rechaza_id_nulo() {
            assertThatThrownBy(() -> new AnimalRef(null, "Firulais", "A-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animal id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("rechaza nombre vacio")
        void rechaza_nombre_vacio(String nombre) {
            assertThatThrownBy(() -> new AnimalRef(2L, nombre, "A-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animal name is required");
        }
    }

    @Nested
    @DisplayName("CompanyRef")
    class Company {

        @Test
        @DisplayName("conserva id, nombre e identificador")
        void conserva_sus_campos() {
            CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "NIT-900");

            assertThat(ref.id()).isEqualTo(9L);
            assertThat(ref.name()).isEqualTo("Clinica Norte");
            assertThat(ref.identifier()).isEqualTo("NIT-900");
        }

        @Test
        @DisplayName("rechaza id nulo")
        void rechaza_id_nulo() {
            assertThatThrownBy(() -> new CompanyRef(null, "Clinica Norte", "NIT-900"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("rechaza nombre vacio")
        void rechaza_nombre_vacio(String nombre) {
            assertThatThrownBy(() -> new CompanyRef(9L, nombre, "NIT-900"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company name is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("rechaza identificador vacio")
        void rechaza_identificador_vacio(String identificador) {
            assertThatThrownBy(() -> new CompanyRef(9L, "Clinica Norte", identificador))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company identifier is required");
        }
    }

    @Nested
    @DisplayName("ConsultationRef")
    class Consultation {

        @Test
        @DisplayName("conserva id y fecha")
        void conserva_sus_campos() {
            ConsultationRef ref = new ConsultationRef(3L, LocalDate.of(2026, 1, 10));

            assertThat(ref.id()).isEqualTo(3L);
            assertThat(ref.date()).isEqualTo(LocalDate.of(2026, 1, 10));
        }

        @Test
        @DisplayName("rechaza id nulo")
        void rechaza_id_nulo() {
            assertThatThrownBy(() -> new ConsultationRef(null, LocalDate.of(2026, 1, 10)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("consultation id is required");
        }

        @Test
        @DisplayName("rechaza fecha nula")
        void rechaza_fecha_nula() {
            assertThatThrownBy(() -> new ConsultationRef(3L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("consultation date is required");
        }
    }

    @Nested
    @DisplayName("VaccinationTypeRef")
    class TipoDeVacuna {

        @Test
        @DisplayName("conserva id y nombre")
        void conserva_sus_campos() {
            VaccinationTypeRef ref = new VaccinationTypeRef(1L, "Rabia");

            assertThat(ref.id()).isEqualTo(1L);
            assertThat(ref.name()).isEqualTo("Rabia");
        }

        @Test
        @DisplayName("rechaza id nulo")
        void rechaza_id_nulo() {
            assertThatThrownBy(() -> new VaccinationTypeRef(null, "Rabia"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("vaccination type id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("rechaza nombre vacio")
        void rechaza_nombre_vacio(String nombre) {
            assertThatThrownBy(() -> new VaccinationTypeRef(1L, nombre))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("vaccination type name is required");
        }
    }

    @Nested
    @DisplayName("VaccinationNotFoundException")
    class NoEncontrada {

        @Test
        @DisplayName("lleva el id en el mensaje para que el 404 diga cual")
        void lleva_el_id_en_el_mensaje() {
            assertThat(new VaccinationNotFoundException(77L))
                    .hasMessageContaining("Vaccination not found: 77");
        }
    }
}
