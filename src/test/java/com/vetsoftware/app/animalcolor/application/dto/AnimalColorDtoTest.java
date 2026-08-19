package com.vetsoftware.app.animalcolor.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.animalcolor.domain.AnimalColor;
import com.vetsoftware.app.animalcolor.domain.SpecieRef;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AnimalColorDto — mapeo desde el dominio")
class AnimalColorDtoTest {

    @Test
    @DisplayName("from() copia cada campo, incluida la especie resumida")
    void from_copia_cada_campo() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        AnimalColor color = new AnimalColor(2L, "Negro", new SpecieRef(1L, "Perro"), creado, null,
                true);

        AnimalColorDto dto = AnimalColorDto.from(color);

        assertThat(dto.id()).isEqualTo(2L);
        assertThat(dto.name()).isEqualTo("Negro");
        assertThat(dto.specie()).isEqualTo(new SpecieSummaryDto(1L, "Perro"));
        assertThat(dto.createdDate()).isEqualTo(creado);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("from() conserva enabled=false de un color deshabilitado")
    void from_conserva_enabled_false_de_un_color_deshabilitado() {
        AnimalColor color = new AnimalColor(2L, "Negro", new SpecieRef(1L, "Perro"),
                LocalDateTime.of(2026, 1, 15, 10, 30), null, false);

        assertThat(AnimalColorDto.from(color).enabled()).isFalse();
    }
}
