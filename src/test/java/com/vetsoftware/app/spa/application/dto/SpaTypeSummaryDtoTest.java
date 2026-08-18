package com.vetsoftware.app.spa.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.spa.testsupport.SpaMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpaTypeSummaryDto.from")
class SpaTypeSummaryDtoTest {

    @Test
    @DisplayName("mapea cada campo del companion VO, campo por campo")
    void mapea_cada_campo() {
        SpaTypeSummaryDto dto = SpaTypeSummaryDto.from(SpaMother.BANO_BASICO);

        assertThat(dto.id()).isEqualTo(SpaMother.BANO_BASICO.id());
        assertThat(dto.name()).isEqualTo(SpaMother.BANO_BASICO.name());
    }
}
