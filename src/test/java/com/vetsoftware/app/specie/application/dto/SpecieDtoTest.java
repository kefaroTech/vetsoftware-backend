package com.vetsoftware.app.specie.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.specie.domain.Specie;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpecieDto")
class SpecieDtoTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Test
    @DisplayName("from copia cada campo de la especie, campo por campo")
    void from_copia_cada_campo() {
        Specie specie = new Specie(1L, "Perro", CREADO, true);

        SpecieDto dto = SpecieDto.from(specie);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Perro");
        assertThat(dto.createdDate()).isEqualTo(CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("from conserva una especie deshabilitada")
    void from_conserva_una_especie_deshabilitada() {
        Specie specie = new Specie(1L, "Perro", CREADO, false);

        assertThat(SpecieDto.from(specie).enabled()).isFalse();
    }
}
