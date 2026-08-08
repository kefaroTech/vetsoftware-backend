package com.vetsoftware.app.animal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
@DisplayName("Companion VOs del modulo animal")
class CompanionRefsTest {

    @Nested
    @DisplayName("AnimalRef")
    class AnimalRefTest {

        @Test
        @DisplayName("acepta id y nombre y expone el codigo tal cual")
        void acepta_id_y_nombre() {
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
    @DisplayName("SpecieRef")
    class SpecieRefTest {

        @Test
        @DisplayName("rechaza id null")
        void rechaza_id_null() {
            assertThatThrownBy(() -> new SpecieRef(null, "Perro"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("specie id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("rechaza nombre vacio")
        void rechaza_nombre_vacio(String nombre) {
            assertThatThrownBy(() -> new SpecieRef(1L, nombre))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("specie name is required");
        }
    }

    @Nested
    @DisplayName("BreedRef")
    class BreedRefTest {

        @Test
        @DisplayName("rechaza id null")
        void rechaza_id_null() {
            assertThatThrownBy(() -> new BreedRef(null, "Labrador"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("breed id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("rechaza nombre vacio")
        void rechaza_nombre_vacio(String nombre) {
            assertThatThrownBy(() -> new BreedRef(1L, nombre))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("breed name is required");
        }
    }

    @Nested
    @DisplayName("OwnerRef")
    class OwnerRefTest {

        @Test
        @DisplayName("rechaza id null")
        void rechaza_id_null() {
            assertThatThrownBy(() -> new OwnerRef(null, "Ana", "CC-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("owner id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("rechaza nombre vacio")
        void rechaza_nombre_vacio(String nombre) {
            assertThatThrownBy(() -> new OwnerRef(1L, nombre, "CC-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("owner name is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("rechaza documento vacio — es la clave con la que se identifica al dueno")
        void rechaza_documento_vacio(String documento) {
            assertThatThrownBy(() -> new OwnerRef(1L, "Ana", documento))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("owner document is required");
        }
    }

    @Nested
    @DisplayName("CompanyRef")
    class CompanyRefTest {

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
    @DisplayName("AnimalColorRef")
    class AnimalColorRefTest {

        @Test
        @DisplayName("rechaza id null")
        void rechaza_id_null() {
            assertThatThrownBy(() -> new AnimalColorRef(null, "Negro"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animalColor id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("rechaza nombre vacio")
        void rechaza_nombre_vacio(String nombre) {
            assertThatThrownBy(() -> new AnimalColorRef(1L, nombre))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animalColor name is required");
        }
    }
}
