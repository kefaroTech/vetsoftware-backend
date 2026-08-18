package com.vetsoftware.app.laboratorytestfile.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestRef;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LaboratoryTestSummaryDto — from()")
class LaboratoryTestSummaryDtoTest {

    @Test
    @DisplayName("copia cada campo del ref")
    void copia_cada_campo_del_ref() {
        LaboratoryTestRef ref = new LaboratoryTestRef(500L, LocalDate.of(2026, 1, 15));

        LaboratoryTestSummaryDto dto = LaboratoryTestSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(500L);
        assertThat(dto.date()).isEqualTo(LocalDate.of(2026, 1, 15));
    }
}
