package com.vetsoftware.app.surgery.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.surgery.domain.SurgeryTypeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SurgeryTypeSummaryDto.from")
class SurgeryTypeSummaryDtoTest {

    @Test
    @DisplayName("copia cada campo del companion VO")
    void copia_cada_campo_del_companion_vo() {
        SurgeryTypeRef ref = new SurgeryTypeRef(5L, "Ovariohisterectomia");

        SurgeryTypeSummaryDto dto = SurgeryTypeSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.name()).isEqualTo("Ovariohisterectomia");
    }
}
