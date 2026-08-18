package com.vetsoftware.app.prescription.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsultationSummaryDto")
class ConsultationSummaryDtoTest {

    @Test
    @DisplayName("from(ConsultationRef) copia cada campo")
    void from_copia_cada_campo() {
        ConsultationSummaryDto dto = ConsultationSummaryDto.from(PrescriptionMother.CONSULTA);

        assertThat(dto.id()).isEqualTo(PrescriptionMother.CONSULTA.id());
        assertThat(dto.date()).isEqualTo(PrescriptionMother.CONSULTA.date());
    }
}
