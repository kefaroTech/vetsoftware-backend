package com.vetsoftware.app.securityincident.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.securityincident.testsupport.SecurityIncidentMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SecurityIncidentDto.from")
class SecurityIncidentDtoTest {

    @Test
    @DisplayName("copia cada campo del incidente cerrado en su posicion")
    void copia_cada_campo_del_incidente_cerrado() {
        SecurityIncidentDto dto = SecurityIncidentDto.from(SecurityIncidentMother.cerrado());

        assertThat(dto.id()).isEqualTo(SecurityIncidentMother.INCIDENT_ID);
        assertThat(dto.detectedAt()).isEqualTo(SecurityIncidentMother.DETECTED_AT);
        assertThat(dto.occurredAt()).isEqualTo(SecurityIncidentMother.OCCURRED_AT);
        assertThat(dto.escalatedAt()).isEqualTo(SecurityIncidentMother.ESCALATED_AT);
        assertThat(dto.kind()).isEqualTo(SecurityIncidentMother.KIND);
        assertThat(dto.severity()).isEqualTo(SecurityIncidentMother.SEVERITY);
        assertThat(dto.summary()).isEqualTo(SecurityIncidentMother.SUMMARY);
        assertThat(dto.affectedSubjectCount())
                .isEqualTo(SecurityIncidentMother.AFFECTED_SUBJECT_COUNT);
        assertThat(dto.deadlineAt()).isEqualTo(SecurityIncidentMother.DEADLINE_AT);
        assertThat(dto.reportedToAuthorityAt()).isEqualTo(SecurityIncidentMother.REPORTED_AT);
        assertThat(dto.reportReference()).isEqualTo(SecurityIncidentMother.REPORT_REFERENCE);
        assertThat(dto.notifiedSubjectsAt()).isEqualTo(SecurityIncidentMother.NOTIFIED_SUBJECTS_AT);
        assertThat(dto.containment()).isEqualTo(SecurityIncidentMother.CONTAINMENT);
        assertThat(dto.rootCause()).isEqualTo(SecurityIncidentMother.ROOT_CAUSE);
        assertThat(dto.closedAt()).isEqualTo(SecurityIncidentMother.CLOSED_AT);
        assertThat(dto.createdDate()).isEqualTo(SecurityIncidentMother.CREATED_DATE);
    }

    @Test
    @DisplayName("un incidente recien registrado deja en null lo que todavia no paso")
    void un_incidente_recien_registrado_deja_en_null_lo_pendiente() {
        SecurityIncidentDto dto = SecurityIncidentDto.from(SecurityIncidentMother.registrado());

        assertThat(dto.reportedToAuthorityAt()).isNull();
        assertThat(dto.reportReference()).isNull();
        assertThat(dto.notifiedSubjectsAt()).isNull();
        assertThat(dto.containment()).isNull();
        assertThat(dto.rootCause()).isNull();
        assertThat(dto.closedAt()).isNull();
    }
}
