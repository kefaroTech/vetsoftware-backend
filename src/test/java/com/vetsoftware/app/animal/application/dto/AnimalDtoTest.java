package com.vetsoftware.app.animal.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.domain.AnimalType;
import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.domain.ReproductiveState;
import com.vetsoftware.app.animal.domain.WeightType;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El mapeo campo a campo se prueba entero a proposito: un intercambio entre dos
 * campos del mismo tipo —{@code name}/{@code code},
 * {@code bod}/{@code deceasedDate}— compila, pasa cualquier test de "no es
 * null", y solo se ve en pantalla.
 */
@DisplayName("AnimalDto.from")
class AnimalDtoTest {

    @Test
    @DisplayName("copia cada campo del agregado en su posicion")
    void copia_cada_campo_del_agregado_en_su_posicion() {
        Animal animal = AnimalMother.perroSano();

        AnimalDto dto = AnimalDto.from(animal);

        assertThat(dto.id()).isEqualTo(AnimalMother.ANIMAL_ID);
        assertThat(dto.name()).isEqualTo("Firulais");
        assertThat(dto.code()).isEqualTo("A-001");
        assertThat(dto.gender()).isEqualTo(Gender.MALE);
        assertThat(dto.animalType()).isEqualTo(AnimalType.NONE);
        assertThat(dto.reproductiveState()).isEqualTo(ReproductiveState.STERILIZED);
        assertThat(dto.bod()).isEqualTo(AnimalMother.NACIMIENTO);
        assertThat(dto.size()).isEqualTo(30);
        assertThat(dto.deceased()).isFalse();
        assertThat(dto.deceasedDate()).isNull();
        assertThat(dto.createdDate()).isEqualTo(AnimalMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("aplana los companion VO en summaries sin perder campos")
    void aplana_los_companion_vo_en_summaries() {
        AnimalDto dto = AnimalDto.from(AnimalMother.perroSano());

        assertThat(dto.specie()).isEqualTo(
                new SpecieSummaryDto(AnimalMother.PERRO.id(), AnimalMother.PERRO.name()));
        assertThat(dto.breed()).isEqualTo(
                new BreedSummaryDto(AnimalMother.LABRADOR.id(), AnimalMother.LABRADOR.name()));
        assertThat(dto.owner()).isEqualTo(new OwnerSummaryDto(AnimalMother.DUENO.id(),
                AnimalMother.DUENO.name(), AnimalMother.DUENO.document()));
        assertThat(dto.color()).isEqualTo(
                new AnimalColorSummaryDto(AnimalMother.NEGRO.id(), AnimalMother.NEGRO.name()));
        assertThat(dto.company()).isEqualTo(new CompanySummaryDto(AnimalMother.CLINICA.id(),
                AnimalMother.CLINICA.name(), AnimalMother.CLINICA.identifier()));
    }

    @Nested
    @DisplayName("peso y unidad mostrada")
    class PesoYUnidad {

        @Test
        @DisplayName("sin registros de peso, weight es null y la unidad es la preferida")
        void sin_registros_de_peso_cae_a_la_unidad_preferida() {
            AnimalDto dto = AnimalDto.from(AnimalMother.perroSano());

            assertThat(dto.weight()).isNull();
            assertThat(dto.weightMeasuredAt()).isNull();
            assertThat(dto.weightType()).isEqualTo(WeightType.KILOGRAMS);
        }

        @Test
        @DisplayName("con peso derivado, weightType es la unidad DEL REGISTRO, no la del animal")
        void con_peso_derivado_manda_la_unidad_del_registro() {
            // El animal prefiere KILOGRAMS pero el ultimo registro se tomo en GRAMS.
            // El DTO expone la unidad del registro para que el numero y su unidad
            // siempre viajen juntos y coherentes.
            Animal animal = AnimalMother.conPesoDerivado(new BigDecimal("12500"), WeightType.GRAMS,
                    LocalDate.of(2026, 2, 1));

            AnimalDto dto = AnimalDto.from(animal);

            assertThat(dto.weight()).isEqualByComparingTo("12500");
            assertThat(dto.weightMeasuredAt()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(dto.weightType()).isEqualTo(WeightType.GRAMS);
            assertThat(animal.getWeightType()).isEqualTo(WeightType.KILOGRAMS);
        }
    }

    @Test
    @DisplayName("propaga fallecimiento con su fecha")
    void propaga_fallecimiento_con_su_fecha() {
        LocalDate fecha = LocalDate.of(2026, 3, 20);

        AnimalDto dto = AnimalDto.from(AnimalMother.fallecido(fecha));

        assertThat(dto.deceased()).isTrue();
        assertThat(dto.deceasedDate()).isEqualTo(fecha);
    }

    @Test
    @DisplayName("propaga el animal deshabilitado")
    void propaga_el_animal_deshabilitado() {
        assertThat(AnimalDto.from(AnimalMother.deshabilitado()).enabled()).isFalse();
    }
}
