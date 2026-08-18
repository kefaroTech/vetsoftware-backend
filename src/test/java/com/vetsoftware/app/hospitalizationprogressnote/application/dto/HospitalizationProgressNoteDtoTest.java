package com.vetsoftware.app.hospitalizationprogressnote.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNote;
import com.vetsoftware.app.hospitalizationprogressnote.testsupport.HospitalizationProgressNoteMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HospitalizationProgressNoteDto")
class HospitalizationProgressNoteDtoTest {

    @Test
    @DisplayName("from() copia cada campo, incluidos los sumarios anidados")
    void from_copia_cada_campo() {
        HospitalizationProgressNote nota = HospitalizationProgressNoteMother.notaEvolucion();

        HospitalizationProgressNoteDto dto = HospitalizationProgressNoteDto.from(nota);

        assertThat(dto.id()).isEqualTo(nota.getId());
        assertThat(dto.description()).isEqualTo(nota.getDescription());
        assertThat(dto.createdDate()).isEqualTo(nota.getCreatedDate());
        assertThat(dto.enabled()).isTrue();
        assertThat(dto.hospitalization().id()).isEqualTo(nota.getHospitalization().id());
        assertThat(dto.hospitalization().date()).isEqualTo(nota.getHospitalization().date());
        assertThat(dto.createdBy().id()).isEqualTo(nota.getCreatedBy().id());
        assertThat(dto.createdBy().employeeCode()).isEqualTo(nota.getCreatedBy().employeeCode());
        assertThat(dto.createdBy().name()).isEqualTo(nota.getCreatedBy().name());
    }

    @Test
    @DisplayName("from() conserva el estado deshabilitado")
    void from_conserva_el_estado_deshabilitado() {
        HospitalizationProgressNoteDto dto = HospitalizationProgressNoteDto
                .from(HospitalizationProgressNoteMother.deshabilitada());

        assertThat(dto.enabled()).isFalse();
    }
}
