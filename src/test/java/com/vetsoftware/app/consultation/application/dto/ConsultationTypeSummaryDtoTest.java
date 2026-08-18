package com.vetsoftware.app.consultation.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.consultation.domain.ConsultationTypeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsultationTypeSummaryDto.from")
class ConsultationTypeSummaryDtoTest {

    @Test
    @DisplayName("copia cada campo del companion VO")
    void copia_cada_campo_del_companion_vo() {
        ConsultationTypeRef ref = new ConsultationTypeRef(5L, "Control");

        ConsultationTypeSummaryDto dto = ConsultationTypeSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.name()).isEqualTo("Control");
    }
}
