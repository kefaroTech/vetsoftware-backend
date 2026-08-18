package com.vetsoftware.app.city.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.city.domain.StateRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StateSummaryDto.from — mapeo campo por campo")
class StateSummaryDtoTest {

    @Test
    @DisplayName("copia cada campo del companion VO")
    void copia_cada_campo_del_companion_vo() {
        StateRef ref = new StateRef(9L, "Antioquia");

        StateSummaryDto dto = StateSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(9L);
        assertThat(dto.name()).isEqualTo("Antioquia");
    }
}
