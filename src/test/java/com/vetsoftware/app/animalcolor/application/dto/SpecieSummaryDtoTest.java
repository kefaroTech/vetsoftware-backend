package com.vetsoftware.app.animalcolor.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.animalcolor.domain.SpecieRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpecieSummaryDto — mapeo desde SpecieRef")
class SpecieSummaryDtoTest {

    @Test
    @DisplayName("from() copia id y nombre de la referencia")
    void from_copia_id_y_nombre_de_la_referencia() {
        SpecieSummaryDto dto = SpecieSummaryDto.from(new SpecieRef(1L, "Perro"));

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Perro");
    }
}
