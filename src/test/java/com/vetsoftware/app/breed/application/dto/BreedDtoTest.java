package com.vetsoftware.app.breed.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.breed.domain.Breed;
import com.vetsoftware.app.breed.domain.SpecieRef;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BreedDto — mapeo desde el dominio")
class BreedDtoTest {

    @Test
    @DisplayName("from() copia cada campo, incluida la especie resumida")
    void from_copia_cada_campo() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        Breed breed = new Breed(2L, "Labrador", new SpecieRef(1L, "Perro"), creado, null, true);

        BreedDto dto = BreedDto.from(breed);

        assertThat(dto.id()).isEqualTo(2L);
        assertThat(dto.name()).isEqualTo("Labrador");
        assertThat(dto.specie()).isEqualTo(new SpecieSummaryDto(1L, "Perro"));
        assertThat(dto.createdDate()).isEqualTo(creado);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("from() conserva enabled=false de una raza deshabilitada")
    void from_conserva_enabled_false_de_una_raza_deshabilitada() {
        Breed breed = new Breed(2L, "Labrador", new SpecieRef(1L, "Perro"),
                LocalDateTime.of(2026, 1, 15, 10, 30), null, false);

        assertThat(BreedDto.from(breed).enabled()).isFalse();
    }
}
