package com.vetsoftware.app.animal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.animal.testsupport.AnimalMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Animal — invariantes y ciclo de vida del agregado")
class AnimalTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir 18
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 1L;
        private String name = "Firulais";
        private String code = "A-001";
        private SpecieRef specie = AnimalMother.PERRO;
        private BreedRef breed = AnimalMother.LABRADOR;
        private OwnerRef owner = AnimalMother.DUENO;
        private Gender gender = Gender.MALE;
        private WeightType weightType = WeightType.KILOGRAMS;
        private AnimalType animalType = AnimalType.NONE;
        private ReproductiveState reproductiveState = ReproductiveState.STERILIZED;
        private AnimalColorRef color = AnimalMother.NEGRO;
        private LocalDate bod = AnimalMother.NACIMIENTO;
        private Integer size = 30;
        private boolean deceased;
        private LocalDate deceasedDate;
        private CompanyRef company = AnimalMother.CLINICA;

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder code(String v) {
            this.code = v;
            return this;
        }

        private Builder specie(SpecieRef v) {
            this.specie = v;
            return this;
        }

        private Builder breed(BreedRef v) {
            this.breed = v;
            return this;
        }

        private Builder owner(OwnerRef v) {
            this.owner = v;
            return this;
        }

        private Builder gender(Gender v) {
            this.gender = v;
            return this;
        }

        private Builder weightType(WeightType v) {
            this.weightType = v;
            return this;
        }

        private Builder animalType(AnimalType v) {
            this.animalType = v;
            return this;
        }

        private Builder reproductiveState(ReproductiveState v) {
            this.reproductiveState = v;
            return this;
        }

        private Builder color(AnimalColorRef v) {
            this.color = v;
            return this;
        }

        private Builder size(Integer v) {
            this.size = v;
            return this;
        }

        private Builder deceased(boolean v) {
            this.deceased = v;
            return this;
        }

        private Builder deceasedDate(LocalDate v) {
            this.deceasedDate = v;
            return this;
        }

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private Animal build() {
            return new Animal(id, name, code, specie, breed, owner, gender, weightType, animalType,
                    reproductiveState, color, bod, size, deceased, deceasedDate, company,
                    AnimalMother.CREADO, true);
        }

        private void applyTo(Animal animal) {
            animal.update(name, code, specie, breed, owner, gender, weightType, animalType,
                    reproductiveState, color, bod, size, deceased, deceasedDate, company);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Animal animal = valido().build();

            assertThat(animal.getId()).isEqualTo(1L);
            assertThat(animal.getName()).isEqualTo("Firulais");
            assertThat(animal.getCode()).isEqualTo("A-001");
            assertThat(animal.getSpecie()).isEqualTo(AnimalMother.PERRO);
            assertThat(animal.getBreed()).isEqualTo(AnimalMother.LABRADOR);
            assertThat(animal.getOwner()).isEqualTo(AnimalMother.DUENO);
            assertThat(animal.getGender()).isEqualTo(Gender.MALE);
            assertThat(animal.getWeightType()).isEqualTo(WeightType.KILOGRAMS);
            assertThat(animal.getAnimalType()).isEqualTo(AnimalType.NONE);
            assertThat(animal.getReproductiveState()).isEqualTo(ReproductiveState.STERILIZED);
            assertThat(animal.getColor()).isEqualTo(AnimalMother.NEGRO);
            assertThat(animal.getBod()).isEqualTo(AnimalMother.NACIMIENTO);
            assertThat(animal.getSize()).isEqualTo(30);
            assertThat(animal.isDeceased()).isFalse();
            assertThat(animal.getDeceasedDate()).isNull();
            assertThat(animal.getCompany()).isEqualTo(AnimalMother.CLINICA);
            assertThat(animal.getCreatedDate()).isEqualTo(AnimalMother.CREADO);
            assertThat(animal.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y sin peso derivado")
        void create_nace_sin_id_habilitado_y_sin_peso_derivado() {
            Animal animal = Animal.create("Firulais", "A-001", AnimalMother.PERRO,
                    AnimalMother.LABRADOR, AnimalMother.DUENO, Gender.MALE, WeightType.KILOGRAMS,
                    AnimalType.NONE, ReproductiveState.STERILIZED, AnimalMother.NEGRO,
                    AnimalMother.NACIMIENTO, 30, false, null, AnimalMother.CLINICA);

            assertThat(animal.getId()).isNull();
            assertThat(animal.isEnabled()).isTrue();
            assertThat(animal.getCurrentWeight()).isNull();
            assertThat(animal.getCurrentWeightUnit()).isNull();
            assertThat(animal.getCurrentWeightMeasuredAt()).isNull();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(animal.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("code es opcional: un animal sin codigo es valido")
        void code_es_opcional() {
            assertThatCode(() -> valido().code(null).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("size es opcional y admite cero")
        void size_es_opcional_y_admite_cero() {
            assertThatCode(() -> valido().size(null).build()).doesNotThrowAnyException();
            assertThatCode(() -> valido().size(0).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null", (ThrowingCallable) () -> valido().name(null).build(),
                            "name is required"),
                    arguments("name vacio", (ThrowingCallable) () -> valido().name("").build(),
                            "name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> valido().name("   ").build(),
                            "name is required"),
                    arguments("name de 101 chars",
                            (ThrowingCallable) () -> valido().name("x".repeat(101)).build(),
                            "name must be 100 chars or less"),
                    arguments("code de 51 chars",
                            (ThrowingCallable) () -> valido().code("x".repeat(51)).build(),
                            "code must be 50 chars or less"),
                    arguments("specie null", (ThrowingCallable) () -> valido().specie(null).build(),
                            "specie is required"),
                    arguments("breed null", (ThrowingCallable) () -> valido().breed(null).build(),
                            "breed is required"),
                    arguments("owner null", (ThrowingCallable) () -> valido().owner(null).build(),
                            "owner is required"),
                    arguments("gender null", (ThrowingCallable) () -> valido().gender(null).build(),
                            "gender is required"),
                    arguments("weightType null",
                            (ThrowingCallable) () -> valido().weightType(null).build(),
                            "weightType is required"),
                    arguments("animalType null",
                            (ThrowingCallable) () -> valido().animalType(null).build(),
                            "animalType is required"),
                    arguments("reproductiveState null",
                            (ThrowingCallable) () -> valido().reproductiveState(null).build(),
                            "reproductiveState is required"),
                    arguments("color null", (ThrowingCallable) () -> valido().color(null).build(),
                            "color is required"),
                    arguments("size negativo", (ThrowingCallable) () -> valido().size(-1).build(),
                            "size cannot be negative"),
                    arguments("company null",
                            (ThrowingCallable) () -> valido().company(null).build(),
                            "company is required"),
                    arguments("fallecido sin fecha",
                            (ThrowingCallable) () -> valido().deceased(true).build(),
                            "deceasedDate is required when deceased is true"),
                    arguments("vivo con fecha de fallecimiento",
                            (ThrowingCallable) () -> valido().deceased(false)
                                    .deceasedDate(LocalDate.of(2026, 1, 1)).build(),
                            "deceasedDate must be null when deceased is false"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @ParameterizedTest(name = "longitud {0}")
        @ValueSource(ints = {1, 100})
        @DisplayName("name en el limite exacto se acepta")
        void name_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(() -> valido().name("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("code de 50 chars se acepta")
        void code_de_50_chars_se_acepta() {
            assertThatCode(() -> valido().code("x".repeat(50)).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fallecido con fecha es el unico par coherente")
        void fallecido_con_fecha_es_el_unico_par_coherente() {
            assertThatCode(
                    () -> valido().deceased(true).deceasedDate(LocalDate.of(2026, 1, 1)).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id y createdDate")
        void reemplaza_los_campos_mutables_y_conserva_id_y_created_date() {
            Animal animal = valido().build();

            valido().name("Michi").code("A-002").specie(AnimalMother.GATO)
                    .breed(AnimalMother.SIAMES).owner(AnimalMother.OTRO_DUENO).gender(Gender.FEMALE)
                    .weightType(WeightType.GRAMS).animalType(AnimalType.SERVICE)
                    .reproductiveState(ReproductiveState.NO_STERILIZED).color(AnimalMother.BLANCO)
                    .size(12).applyTo(animal);

            assertThat(animal.getName()).isEqualTo("Michi");
            assertThat(animal.getCode()).isEqualTo("A-002");
            assertThat(animal.getSpecie()).isEqualTo(AnimalMother.GATO);
            assertThat(animal.getBreed()).isEqualTo(AnimalMother.SIAMES);
            assertThat(animal.getOwner()).isEqualTo(AnimalMother.OTRO_DUENO);
            assertThat(animal.getGender()).isEqualTo(Gender.FEMALE);
            assertThat(animal.getWeightType()).isEqualTo(WeightType.GRAMS);
            assertThat(animal.getAnimalType()).isEqualTo(AnimalType.SERVICE);
            assertThat(animal.getReproductiveState()).isEqualTo(ReproductiveState.NO_STERILIZED);
            assertThat(animal.getColor()).isEqualTo(AnimalMother.BLANCO);
            assertThat(animal.getSize()).isEqualTo(12);
            assertThat(animal.getId()).isEqualTo(1L);
            assertThat(animal.getCreatedDate()).isEqualTo(AnimalMother.CREADO);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            Animal animal = valido().build();

            // El nombre es valido y la especie no: si validate() no corriera ANTES de
            // asignar, el animal se quedaria con el nombre nuevo y la especie vieja.
            assertThatThrownBy(() -> valido().name("Michi").specie(null).applyTo(animal))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(animal.getName()).isEqualTo("Firulais");
            assertThat(animal.getSpecie()).isEqualTo(AnimalMother.PERRO);
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            Animal animal = valido().build();
            animal.disable();

            valido().name("Michi").applyTo(animal);

            assertThat(animal.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("peso actual derivado")
    class PesoDerivado {

        @Test
        @DisplayName("applyCurrentWeight hidrata los tres campos derivados")
        void apply_current_weight_hidrata_los_tres_campos_derivados() {
            Animal animal = valido().build();

            animal.applyCurrentWeight(new BigDecimal("12.50"), WeightType.KILOGRAMS,
                    LocalDate.of(2026, 2, 1));

            assertThat(animal.getCurrentWeight()).isEqualByComparingTo("12.50");
            assertThat(animal.getCurrentWeightUnit()).isEqualTo(WeightType.KILOGRAMS);
            assertThat(animal.getCurrentWeightMeasuredAt()).isEqualTo(LocalDate.of(2026, 2, 1));
        }

        @Test
        @DisplayName("update no borra el peso derivado")
        void update_no_borra_el_peso_derivado() {
            Animal animal = valido().build();
            animal.applyCurrentWeight(new BigDecimal("12.50"), WeightType.KILOGRAMS,
                    LocalDate.of(2026, 2, 1));

            valido().name("Michi").weightType(WeightType.GRAMS).applyTo(animal);

            // El peso es serie temporal: lo unico que cambia es la unidad PREFERIDA.
            assertThat(animal.getCurrentWeight()).isEqualByComparingTo("12.50");
            assertThat(animal.getCurrentWeightUnit()).isEqualTo(WeightType.KILOGRAMS);
            assertThat(animal.getWeightType()).isEqualTo(WeightType.GRAMS);
        }

        @Test
        @DisplayName("admite limpiar el peso derivado con nulls")
        void admite_limpiar_el_peso_derivado_con_nulls() {
            Animal animal = valido().build();
            animal.applyCurrentWeight(new BigDecimal("12.50"), WeightType.KILOGRAMS,
                    LocalDate.of(2026, 2, 1));

            animal.applyCurrentWeight(null, null, null);

            assertThat(animal.getCurrentWeight()).isNull();
            assertThat(animal.getCurrentWeightUnit()).isNull();
            assertThat(animal.getCurrentWeightMeasuredAt()).isNull();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            Animal animal = valido().build();

            animal.disable();
            assertThat(animal.isEnabled()).isFalse();
            animal.disable();
            assertThat(animal.isEnabled()).isFalse();

            animal.enable();
            assertThat(animal.isEnabled()).isTrue();
            animal.enable();
            assertThat(animal.isEnabled()).isTrue();
        }
    }
}
