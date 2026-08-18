package com.vetsoftware.app.clinicalhistory.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEvent;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import com.vetsoftware.app.clinicalhistory.testsupport.ClinicalHistoryMother;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ClinicalEventDto.from — mapea campo por campo")
class ClinicalEventDtoTest {

    @Test
    @DisplayName("copia cada campo del ClinicalEvent de dominio")
    void copia_cada_campo() {
        ClinicalEvent evento = new ClinicalEvent(500L, ClinicalHistoryMother.ANIMAL_ID,
                ClinicalHistoryMother.COMPANY_ID, 500L, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3), ClinicalEventType.HOSPITALIZATION, "Resumen");

        ClinicalEventDto dto = ClinicalEventDto.from(evento);

        assertThat(dto.sourceId()).isEqualTo(evento.sourceId());
        assertThat(dto.animalId()).isEqualTo(evento.animalId());
        assertThat(dto.eventType()).isEqualTo(evento.eventType());
        assertThat(dto.eventDate()).isEqualTo(evento.eventDate());
        assertThat(dto.endDate()).isEqualTo(evento.endDate());
        assertThat(dto.consultationId()).isEqualTo(evento.consultationId());
        assertThat(dto.summary()).isEqualTo(evento.summary());
    }

    @Test
    @DisplayName("conserva los opcionales en null")
    void conserva_opcionales_en_null() {
        ClinicalEvent evento = ClinicalHistoryMother.consulta();
        ClinicalEvent sinExtras = new ClinicalEvent(evento.sourceId(), evento.animalId(),
                evento.companyId(), null, evento.eventDate(), null, evento.eventType(), null);

        ClinicalEventDto dto = ClinicalEventDto.from(sinExtras);

        assertThat(dto.consultationId()).isNull();
        assertThat(dto.endDate()).isNull();
        assertThat(dto.summary()).isNull();
    }
}
