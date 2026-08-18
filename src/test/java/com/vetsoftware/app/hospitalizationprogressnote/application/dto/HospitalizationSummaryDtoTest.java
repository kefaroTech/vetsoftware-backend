package com.vetsoftware.app.hospitalizationprogressnote.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationRef;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HospitalizationSummaryDto")
class HospitalizationSummaryDtoTest {

    @Test
    @DisplayName("from() copia id y fecha del companion VO")
    void from_copia_cada_campo() {
        HospitalizationRef ref = new HospitalizationRef(55L, LocalDate.of(2026, 3, 1));

        HospitalizationSummaryDto dto = HospitalizationSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(55L);
        assertThat(dto.date()).isEqualTo(LocalDate.of(2026, 3, 1));
    }
}
