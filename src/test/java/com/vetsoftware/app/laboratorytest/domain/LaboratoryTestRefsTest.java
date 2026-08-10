package com.vetsoftware.app.laboratorytest.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Los companion VO son la frontera del vertical slicing: si dejan entrar un
 * dato incompleto de otra feature, el fallo aparece mucho mas tarde y lejos del
 * origen.
 */
@DisplayName("Companion VOs y catalogos del modulo laboratorytest")
class LaboratoryTestRefsTest {

    @Nested
    @DisplayName("AnimalRef")
    class Animal {

        @Test
        @DisplayName("expone id, nombre y codigo tal cual")
        void expone_los_tres_campos() {
            AnimalRef ref = new AnimalRef(7L, "Firulais", "A-001");

            assertThat(ref.id()).isEqualTo(7L);
            assertThat(ref.name()).isEqualTo("Firulais");
            assertThat(ref.code()).isEqualTo("A-001");
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
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("rechaza el nombre en blanco")
        void rechaza_el_nombre_en_blanco(String nombre) {
            assertThatThrownBy(() -> new AnimalRef(7L, nombre, "A-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animal name is required");
        }

        @Test
        @DisplayName("el codigo es opcional: hay animales sin codigo asignado")
        void el_codigo_es_opcional() {
            assertThatCode(() -> new AnimalRef(7L, "Firulais", null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("dos referencias con los mismos datos son iguales")
        void dos_referencias_con_los_mismos_datos_son_iguales() {
            assertThat(new AnimalRef(7L, "Firulais", "A-001"))
                    .isEqualTo(new AnimalRef(7L, "Firulais", "A-001"));
        }
    }

    @Nested
    @DisplayName("CompanyRef")
    class Company {

        @Test
        @DisplayName("expone id, nombre e identificador")
        void expone_los_tres_campos() {
            CompanyRef ref = new CompanyRef(9L, "Clinica Kefaro", "900123456");

            assertThat(ref.id()).isEqualTo(9L);
            assertThat(ref.name()).isEqualTo("Clinica Kefaro");
            assertThat(ref.identifier()).isEqualTo("900123456");
        }

        @Test
        @DisplayName("rechaza id nulo")
        void rechaza_id_nulo() {
            assertThatThrownBy(() -> new CompanyRef(null, "Clinica Kefaro", "900123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("rechaza el nombre en blanco")
        void rechaza_el_nombre_en_blanco(String nombre) {
            assertThatThrownBy(() -> new CompanyRef(9L, nombre, "900123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company name is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("rechaza el identificador en blanco: es el NIT que sale en el informe")
        void rechaza_el_identificador_en_blanco(String identificador) {
            assertThatThrownBy(() -> new CompanyRef(9L, "Clinica Kefaro", identificador))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company identifier is required");
        }
    }

    @Nested
    @DisplayName("ConsultationRef")
    class Consultation {

        @Test
        @DisplayName("expone id y fecha")
        void expone_id_y_fecha() {
            ConsultationRef ref = new ConsultationRef(11L, LocalDate.of(2026, 3, 14));

            assertThat(ref.id()).isEqualTo(11L);
            assertThat(ref.date()).isEqualTo(LocalDate.of(2026, 3, 14));
        }

        @Test
        @DisplayName("rechaza id nulo")
        void rechaza_id_nulo() {
            assertThatThrownBy(() -> new ConsultationRef(null, LocalDate.of(2026, 3, 14)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("consultation id is required");
        }

        @Test
        @DisplayName("rechaza fecha nula")
        void rechaza_fecha_nula() {
            assertThatThrownBy(() -> new ConsultationRef(11L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("consultation date is required");
        }
    }

    @Nested
    @DisplayName("EmployeeRef")
    class Employee {

        @Test
        @DisplayName("expone id, codigo y nombre")
        void expone_los_tres_campos() {
            EmployeeRef ref = new EmployeeRef(3L, "EMP-003", "Ana Ruiz");

            assertThat(ref.id()).isEqualTo(3L);
            assertThat(ref.employeeCode()).isEqualTo("EMP-003");
            assertThat(ref.name()).isEqualTo("Ana Ruiz");
        }

        @Test
        @DisplayName("rechaza id nulo")
        void rechaza_id_nulo() {
            assertThatThrownBy(() -> new EmployeeRef(null, "EMP-003", "Ana Ruiz"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("rechaza el codigo en blanco: es la firma del informe")
        void rechaza_el_codigo_en_blanco(String codigo) {
            assertThatThrownBy(() -> new EmployeeRef(3L, codigo, "Ana Ruiz"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee code is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("rechaza el nombre en blanco")
        void rechaza_el_nombre_en_blanco(String nombre) {
            assertThatThrownBy(() -> new EmployeeRef(3L, "EMP-003", nombre))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee name is required");
        }
    }

    @Nested
    @DisplayName("LaboratoryTestTypeRef")
    class TestType {

        @Test
        @DisplayName("expone id y nombre")
        void expone_id_y_nombre() {
            LaboratoryTestTypeRef ref = new LaboratoryTestTypeRef(4L, "Hemograma");

            assertThat(ref.id()).isEqualTo(4L);
            assertThat(ref.name()).isEqualTo("Hemograma");
        }

        @Test
        @DisplayName("rechaza id nulo")
        void rechaza_id_nulo() {
            assertThatThrownBy(() -> new LaboratoryTestTypeRef(null, "Hemograma"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("test type id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("rechaza el nombre en blanco")
        void rechaza_el_nombre_en_blanco(String nombre) {
            assertThatThrownBy(() -> new LaboratoryTestTypeRef(4L, nombre))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("test type name is required");
        }
    }

    @Nested
    @DisplayName("Catalogos")
    class Catalogos {

        @ParameterizedTest
        @EnumSource(LaboratoryTestStatus.class)
        @DisplayName("cada estado se reconstruye desde su nombre persistido")
        void cada_estado_se_reconstruye_desde_su_nombre(LaboratoryTestStatus estado) {
            assertThat(LaboratoryTestStatus.valueOf(estado.name())).isSameAs(estado);
        }

        @Test
        @DisplayName("el catalogo de estados es el pactado con la bandeja de muestras")
        void el_catalogo_de_estados_es_el_pactado() {
            assertThat(LaboratoryTestStatus.values()).containsExactly(
                    LaboratoryTestStatus.PENDING_COLLECTION,
                    LaboratoryTestStatus.PENDING_PROCESSING, LaboratoryTestStatus.IN_PROGRESS,
                    LaboratoryTestStatus.PENDING_VALIDATION, LaboratoryTestStatus.COMPLETED,
                    LaboratoryTestStatus.CANCELLED);
        }

        @Test
        @DisplayName("un estado desconocido no se puede materializar")
        void un_estado_desconocido_no_se_puede_materializar() {
            assertThatThrownBy(() -> LaboratoryTestStatus.valueOf("ARCHIVED"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No enum constant");
        }

        @ParameterizedTest
        @EnumSource(LaboratoryTestPriority.class)
        @DisplayName("cada prioridad se reconstruye desde su nombre persistido")
        void cada_prioridad_se_reconstruye_desde_su_nombre(LaboratoryTestPriority prioridad) {
            assertThat(LaboratoryTestPriority.valueOf(prioridad.name())).isSameAs(prioridad);
        }

        @Test
        @DisplayName("solo hay dos prioridades: normal y urgente")
        void solo_hay_dos_prioridades() {
            assertThat(LaboratoryTestPriority.values())
                    .containsExactly(LaboratoryTestPriority.NORMAL, LaboratoryTestPriority.URGENTE);
        }
    }

    @Nested
    @DisplayName("LaboratoryTestNotFoundException")
    class NoEncontrada {

        @Test
        @DisplayName("el mensaje lleva el id buscado para poder rastrearlo en el log")
        void el_mensaje_lleva_el_id_buscado() {
            assertThat(new LaboratoryTestNotFoundException(42L))
                    .hasMessage("LaboratoryTest not found: 42");
        }

        @Test
        @DisplayName("es una excepcion no comprobada: no obliga a propagar throws")
        void es_una_excepcion_no_comprobada() {
            assertThat(new LaboratoryTestNotFoundException(1L))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
